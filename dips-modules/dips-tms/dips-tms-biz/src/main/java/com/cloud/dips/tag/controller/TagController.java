package com.cloud.dips.tag.controller;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.validation.Valid;

import com.cloud.dips.common.log.enmu.EnumRole;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.plugins.Page;
import com.cloud.dips.common.core.util.Query;
import com.cloud.dips.common.core.util.R;
import com.cloud.dips.common.log.annotation.SysLog;
import com.cloud.dips.common.security.service.DipsUser;
import com.cloud.dips.common.security.util.SecurityUtils;
import com.cloud.dips.tag.api.dto.GovTagDTO;
import com.cloud.dips.tag.api.entity.GovTag;
import com.cloud.dips.tag.api.entity.GovTagMergeRecord;
import com.cloud.dips.tag.api.entity.GovTagModificationRecord;
import com.cloud.dips.tag.api.entity.GovTagRelation;
import com.cloud.dips.tag.api.vo.GovTagVO;
import com.cloud.dips.tag.api.vo.UserTagVO;
import com.cloud.dips.tag.service.GovTagDescriptionService;
import com.cloud.dips.tag.service.GovTagMergeRecordService;
import com.cloud.dips.tag.service.GovTagModificationRecordService;
import com.cloud.dips.tag.service.GovTagRelationService;
import com.cloud.dips.tag.service.GovTagService;
import com.cloud.dips.tag.service.GovTagTypeRelationService;
import com.google.common.collect.Maps;
import com.hankcs.hanlp.HanLP;

import cn.hutool.core.util.StrUtil;
import io.swagger.annotations.ApiOperation;

/**
 * 
 * @author ZB
 *
 */
@RestController
@RequestMapping("/tag")
public class TagController {
	@Autowired
	private GovTagService service;
	
	@Autowired
	private GovTagDescriptionService govTagDescriptionService;
	
	@Autowired
	private GovTagRelationService govTagRelationService;
	
	@Autowired
	private GovTagModificationRecordService recordService;
	
	@Autowired
	private GovTagMergeRecordService mergeRecordService;
	
	@Autowired
	private GovTagRelationService relationService;
	
	@Autowired
	private GovTagTypeRelationService govTagTypeRelationService;
	
	
	
	/**
	 * 
	 * ??????ID????????????
	 *
	 * @param id
	 * 
	 * @return ??????
	 * 
	 */
	@GetMapping("/{id}")
	@SysLog(value="??????????????????",role = EnumRole.WEB_TYE)
	@ApiOperation(value = "??????????????????", notes = "??????ID??????????????????: params{??????ID: id}",httpMethod="GET")
	public GovTagVO tag(@PathVariable Integer id) {
		GovTagVO bean=service.selectGovTagVoById(id);
		bean.addTypeObjs();
		return bean;
	}
	
	/**
	 * ????????????????????????
	 * @param name ????????????
	 * @param id ??????id
	 * @return
	 */
	@PostMapping("/check")
	@ApiOperation(value = "????????????", notes = "????????????",httpMethod="POST")
	public R<Boolean> check(@RequestBody Map<String, Object> params) {
		String name=params.getOrDefault("name", "").toString();
		Integer i=service.findByGovTagName(name);
			if(i<1){
				return new R<Boolean>(Boolean.TRUE);
			}else{
				return new R<Boolean>(Boolean.FALSE);
			}
	}	
	
	/**
	 * ??????????????????
	 *
	 * @param params ?????????
	 * 
	 * @return ????????????
	 */
	@GetMapping("/page")

	@ApiOperation(value = "??????????????????", notes = "????????????",httpMethod="GET")
	public Page<GovTagVO> tagPage(@RequestParam Map<String, Object> params) {
		String orderByField = "orderByField";
		//???????????????
		String fob = "fob";
		if(StrUtil.isBlank(params.getOrDefault(orderByField, "").toString())){
			params.put(orderByField, "id");
		}
		if(StrUtil.isBlank(params.getOrDefault(fob, "").toString())){
			params.put(fob, "b");
		}
		return service.selectAllPage(new Query<>(params));
	}

	/**
	 * ????????????
	 *
	 * @param id
	 * 
	 * @return Rviews
	 */
	@SysLog("????????????")
	@DeleteMapping("/{id}")
	@PreAuthorize("@pms.hasPermission('gov_tag_del')")
	@ApiOperation(value = "????????????", notes = "??????ID????????????: params{??????ID: tagId}",httpMethod="POST")
	public R<Boolean> tagDel(@PathVariable Integer id) {
		GovTag govTag = service.selectById(id);
		govTagTypeRelationService.deleteById(id);
		if(govTag==null){
			return new R<Boolean>(Boolean.FALSE);
		}else{
				govTagDescriptionService.deleteByTagId(govTag.getTagId());
				govTagRelationService.deleteById(govTag.getTagId());
				govTagRelationService.deleteTagRelation(govTag.getTagId(), "tag");
				EntityWrapper<GovTagModificationRecord> er=new EntityWrapper<GovTagModificationRecord>();
				er.eq("tag_id", govTag.getTagId());
				recordService.delete(er);
				EntityWrapper<GovTagMergeRecord> em=new EntityWrapper<GovTagMergeRecord>();
				em.eq("tag_id", govTag.getTagId()).or().eq("merge_id", govTag.getTagId());
				mergeRecordService.delete(em);
				return new R<Boolean>(service.deleteGovTagById(govTag));
		}
	}
	
	@SysLog("????????????")
	@PostMapping("/create")
	@PreAuthorize("@pms.hasPermission('gov_tag_add')")
	@ApiOperation(value = "????????????", notes = "????????????", httpMethod = "POST")
	public R<Boolean> saveTag(@Valid @RequestBody GovTagDTO govTagDto) {
		Integer i=service.findByGovTagName(govTagDto.getName());
		if(i<1){
			GovTag govTag = new GovTag();
			BeanUtils.copyProperties(govTagDto, govTag);
			// ?????????????????? 
			DipsUser user = SecurityUtils.getUser();
			govTag.setCreatorId(user.getId());
			govTag=service.save(govTag,govTagDto.getTypeIds());
			String[] relationTags=govTagDto.getTagList();
			StringBuilder tagKeyWords=new StringBuilder();
			for(String relation:relationTags){
				tagKeyWords.append(relation+",");
			}
			Map<String, Object> params=new HashMap<String, Object>(0);
			params.put("relationId", govTag.getTagId());
			params.put("node", "tag");
			params.put("tagKeyWords", tagKeyWords.toString());
			relationService.saveTagRelation(params);
			return new R<Boolean>(Boolean.TRUE);
		}else{
			return new R<Boolean>(Boolean.FALSE,"???????????????");
		}
		
	}	
	
	/**
	 * ?????????????????????
	 * @param id??????ID
	 * @return
	 */
	@PostMapping("/views/{id}")
	@SysLog(value="?????????????????????",role = EnumRole.WEB_TYE)
	@ApiOperation(value = "?????????????????????", notes = "?????????????????????", httpMethod = "POST")
	public R<Boolean> tagViews(@PathVariable String id) {
		Pattern pattern = Pattern.compile("^[-\\+]?[\\d]*$");
		boolean matches = pattern.matcher(id).matches();
		if (matches) {
			GovTag govTag = service.selectById(id);
			if(govTag==null){
				return new R<Boolean>(Boolean.FALSE,"???????????????");	
			}else{
				govTag.setViews(govTag.getViews()+1);
				return new R<Boolean>(service.updateById(govTag));	
			}	
		}
		return new R<>(Boolean.FALSE,"????????????????????????????????????????????????????????????");
	}
	
	@SysLog("????????????")
	@PostMapping("/update")
	@PreAuthorize("@pms.hasPermission('gov_tag_edit')")
	@ApiOperation(value = "????????????", notes = "????????????", httpMethod = "POST")
	public R<Boolean> updateTag(@Valid @RequestBody GovTagDTO govTagDto) {
		GovTag govTag = service.selectById(govTagDto.getTagId());
		if(StrUtil.equals(govTagDto.getName(), govTag.getName())){
			String[] relationTags=govTagDto.getTagList();
			StringBuilder tagKeyWords=new StringBuilder();
			for(String relation:relationTags){
				tagKeyWords.append(relation+",");
			}
			Map<String, Object> params=new HashMap<String, Object>(0);
			params.put("relationId", govTag.getTagId());
			params.put("node", "tag");
			params.put("tagKeyWords", tagKeyWords.toString());
			relationService.saveTagRelation(params);
			
			BeanUtils.copyProperties(govTagDto, govTag);
			govTag.setUpdateTime(new Date());
			govTagTypeRelationService.saveTagTypeRelation(govTag.getTagId(), govTagDto.getTypeIds());
			return new R<Boolean>(service.updateById(govTag));
		}else{
			Integer i=service.findByGovTagName(govTagDto.getName());
			i=0;
			if(i<1){
				DipsUser user = SecurityUtils.getUser();
				GovTagModificationRecord record=new GovTagModificationRecord();
				record.setCreatorId(user.getId());
				record.setTagId(govTag.getTagId());
				record.setDescription("???"+govTag.getName()+"???????????????"+govTagDto.getName()+"???");
				recordService.insert(record);

				String[] relationTags=govTagDto.getTagList();
				StringBuilder tagKeyWords=new StringBuilder();
				for(String relation:relationTags){
					tagKeyWords.append(relation+",");
				}
				Map<String, Object> params=new HashMap<String, Object>(0);
				params.put("relationId", govTag.getTagId());
				params.put("node", "tag");
				params.put("tagKeyWords", tagKeyWords.toString());
				relationService.saveTagRelation(params);

				BeanUtils.copyProperties(govTagDto, govTag);
				govTag.setUpdateTime(new Date());
				govTagTypeRelationService.saveTagTypeRelation(govTag.getTagId(), govTagDto.getTypeIds());
				return new R<Boolean>(service.updateById(govTag));
			}else{
				return new R<Boolean>(Boolean.FALSE,"??????????????????");
			}	
		}
	}
	
	@PostMapping("/review")
	@PreAuthorize("@pms.hasPermission('gov_tag_edit')")
	@ApiOperation(value = "??????????????????", notes = "??????????????????", httpMethod = "POST")
	public R<Boolean> review(@RequestBody(required=false) List<Integer> ids) {
		if(null != ids && ids.size()>0){
			EntityWrapper<GovTag> e = new EntityWrapper<GovTag>();
			e.in("id", ids);
			return new R<Boolean>(service.updateForSet("status = 1", e));
		}else{
			return new R<Boolean>(Boolean.FALSE,"??????????????????????????????");
		}
	}
	
	@PostMapping("/disable")
	@PreAuthorize("@pms.hasPermission('gov_tag_edit')")
	@ApiOperation(value = "??????????????????", notes = "??????????????????", httpMethod = "POST")
	public R<Boolean> disable(@RequestBody(required=false) List<Integer> ids) {
		if(null != ids && ids.size()>0){
			EntityWrapper<GovTag> e = new EntityWrapper<GovTag>();
			e.in("id", ids);
			return new R<Boolean>(service.updateForSet("status = 0", e));
		}else{
			return new R<Boolean>(Boolean.FALSE,"??????????????????????????????");
		}
	}
	
	@SysLog("??????????????????")
	@PostMapping("/delete")
	@PreAuthorize("@pms.hasPermission('gov_tag_del')")
	@ApiOperation(value = "??????????????????", notes = "??????????????????", httpMethod = "POST")
	public R<Boolean> delete(@RequestBody List<Integer> ids) {
		for(Integer id:ids){
			GovTag govTag = service.selectById(id);
			govTagTypeRelationService.deleteById(id);
			if(govTag!=null){
					govTagDescriptionService.deleteByTagId(govTag.getTagId());
					govTagRelationService.deleteById(govTag.getTagId());
					govTagRelationService.deleteTagRelation(govTag.getTagId(), "tag");
					EntityWrapper<GovTagModificationRecord> er=new EntityWrapper<GovTagModificationRecord>();
					er.eq("tag_id", govTag.getTagId());
					recordService.delete(er);
					EntityWrapper<GovTagMergeRecord> em=new EntityWrapper<GovTagMergeRecord>();
					em.eq("tag_id", govTag.getTagId()).or().eq("merge_id", govTag.getTagId());
					mergeRecordService.delete(em);
					service.deleteGovTagById(govTag);
			}
		}
		return new R<Boolean>(Boolean.TRUE);
	}

	@SuppressWarnings("unchecked")
	@SysLog("????????????")
	@PostMapping("/merge")
	@ApiOperation(value = "????????????", notes = "????????????", httpMethod = "POST")
	public R<Boolean> tagMerge(@RequestBody Map<String, Object> param) {
		Integer mainId=(Integer) param.get("mainId");
		List<Integer> mergeIds=(List<Integer>) param.get("mergeIds");
		GovTag mainTag = service.selectById(mainId);
		if (mainTag.getStatus() == 1 && mainTag.getEnable() == 1) {
			for (Integer mergeId : mergeIds) {
				GovTag mergeTag = service.selectById(mergeId);
				// ????????????????????????
				EntityWrapper<GovTagRelation> govTagRelation = new EntityWrapper<GovTagRelation>();
				govTagRelation.eq("tag_id", mergeId);
				govTagRelationService.updateForSet("tag_id="+"\""+mainId+"\"",govTagRelation);
				// ????????????????????????????????????
				EntityWrapper<GovTag> govTag = new EntityWrapper<GovTag>();
				govTag.eq("id", mergeId);
				service.updateForSet("status=0",govTag);
				// ????????????????????????????????????
				Map<String,Object> map = new HashMap<String,Object>();
				map.put("tag_id", mainId);
				map.put("merge_id", mergeId);
				List<GovTagMergeRecord> selectByMap = mergeRecordService.selectByMap(map);
				if (selectByMap.size() == 0) {
					GovTagMergeRecord govTagMergeRecord = new GovTagMergeRecord();
					govTagMergeRecord.setTagId(mainId);
					govTagMergeRecord.setMergeId(mergeId);
					mergeRecordService.insert(govTagMergeRecord);
				}
				// ?????????????????????????????????
				Integer userId = SecurityUtils.getUser().getId();
				GovTagModificationRecord record=new GovTagModificationRecord();
				record.setCreatorId(userId);
				record.setTagId(mainId);
				record.setDescription("???"+mainTag.getName()+"?????????"+mergeTag.getName()+"?????????");
				recordService.insert(record);
				// ????????????????????????????????????
				EntityWrapper<GovTagRelation> tagRelation = new EntityWrapper<GovTagRelation>();
				tagRelation.eq("tag_id", mainId);
				int refersCount = govTagRelationService.selectCount(tagRelation);
				GovTag tag = new GovTag();
				tag.setRefers(refersCount);
				tag.setTagId(mainId);
				service.updateById(tag);
			}
			return new R<Boolean>(true);
		}
		
		return new R<Boolean>(false);
	}
	

	@GetMapping("/extract_tag")
	@ApiOperation(value = "????????????", notes = "????????????????????????", httpMethod = "GET")
	public List<String> extractTag(@RequestParam String content,@RequestParam Integer num) {
		List<String> keywordList = HanLP.extractKeyword(content, num);
		return keywordList;
	}
	
	@GetMapping("/assn_tag")
	@ApiOperation(value = "????????????", notes = "????????????", httpMethod = "GET")
	public List<Object> assnTag(@RequestParam String keyWord) {
		EntityWrapper<GovTag> e = new EntityWrapper<GovTag>();
		e.setSqlSelect("name");
		e.like("name", keyWord);
		return service.selectObjs(e);
	}

	/**
	 * ????????????????????????web???????????????????????????
	 */
	@GetMapping("/selectAllTags")
	@ApiOperation(value = "????????????????????????web???????????????????????????", notes = "????????????????????????web???????????????????????????", httpMethod = "GET")
	public List<UserTagVO> selectAllTags(@RequestParam String q){
		return service.selectAllTags(q);
	}
	
	/**
	 * ??????ids??????????????????
	 */
	@GetMapping("/selectTagsByIds")
	public List<UserTagVO> selectTagsByIds(String ids){
		return service.selectTagsByIds(ids);
	}
}
