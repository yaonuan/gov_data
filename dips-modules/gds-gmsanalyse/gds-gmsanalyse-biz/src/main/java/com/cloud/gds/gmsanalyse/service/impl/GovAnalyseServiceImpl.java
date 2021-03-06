package com.cloud.gds.gmsanalyse.service.impl;

import com.cloud.dips.gov.api.entity.GovPolicyGeneral;
import com.cloud.dips.gov.api.feign.RemoteGovPolicyGeneralService;
import com.cloud.gds.gmsanalyse.bo.DeconstructionListBo;
import com.cloud.gds.gmsanalyse.constant.AnalyseConstant;
import com.cloud.gds.gmsanalyse.dto.GovPolicyDto;
import com.cloud.gds.gmsanalyse.entity.PolicyAnalyse;
import com.cloud.gds.gmsanalyse.entity.PolicyAnalyseFeature;
import com.cloud.gds.gmsanalyse.entity.PolicyDeconstruction;
import com.cloud.gds.gmsanalyse.service.*;
import com.cloud.gds.gmsanalyse.utils.SerializeUtils;
import com.cloud.gds.gmsanalyse.vo.Analyse;
import com.cloud.gds.gmsanalyse.vo.Child;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @Author : yaonuan
 * @Email : 806039077@qq.com
 * @Date : 2019-04-04
 */
@Service
public class GovAnalyseServiceImpl implements GovAnalyseService {

	private final PolicyAnalyseService policyAnalyseService;
	private final PolicyAnalyseFeatureService policyAnalyseFeatureService;
	private final PolicyDeconstructionService policyDeconstructionService;
	private final AnalyseDeconstruction analyseDeconstruction;
	private final AnalyseAlgorithmService analyseAlgorithmService;
	private final RemoteGovPolicyGeneralService remoteGovPolicyGeneralService;

	@Autowired
	public GovAnalyseServiceImpl(PolicyAnalyseService policyAnalyseService, PolicyAnalyseFeatureService policyAnalyseFeatureService, RemoteGovPolicyGeneralService remoteGovPolicyGeneralService, PolicyDeconstructionService policyDeconstructionService, AnalyseDeconstruction analyseDeconstruction, AnalyseAlgorithmService analyseAlgorithmService) {
		this.policyAnalyseService = policyAnalyseService;
		this.policyAnalyseFeatureService = policyAnalyseFeatureService;
		this.remoteGovPolicyGeneralService = remoteGovPolicyGeneralService;
		this.policyDeconstructionService = policyDeconstructionService;
		this.analyseDeconstruction = analyseDeconstruction;
		this.analyseAlgorithmService = analyseAlgorithmService;
	}

	@Override
	public void govAnalyse(GovPolicyDto govPolicyDto) throws IOException, ClassNotFoundException {
		// ??????????????????
		PolicyAnalyse policyAnalyse = policyAnalyseService.save(govPolicyDto);
		// ??????gms???????????????????????????????????????
		List<Long> ids = remoteGovPolicyGeneralService.gainList(govPolicyDto.getParams());
		// ????????????????????????
		List<Long> longList = new ArrayList<>();
		if (ids.size() > 0) {
			longList = policyDeconstructionService.deconstructionNonExistent(ids);
		}
		// ????????????????????????
		if (longList.size() > 0) {
			List<GovPolicyGeneral> generals = remoteGovPolicyGeneralService.selectByIds(longList);
			//	???????????????????????? TODO 2019-4-25 9:19:16
			for (GovPolicyGeneral general : generals) {
				byte[] bytes = new byte[0];
				ArrayList<String> list = analyseDeconstruction.paragraph_analyse(general.getText());
				try {
					bytes = SerializeUtils.serializeObject(list);
				} catch (IOException e) {
					e.printStackTrace();
				}
				PolicyDeconstruction deconstruction = new PolicyDeconstruction();
				deconstruction.setVerbs(bytes);
				deconstruction.setPolicyId(Long.valueOf(general.getId()));
				deconstruction.setPolicyTitle(general.getTitle());
				// ????????????????????????????????????
				if (policyDeconstructionService.selectByPolicyId(deconstruction.getPolicyId()) == null){
					policyDeconstructionService.insert(deconstruction);
				}
			}
		}
		// ???????????????????????? 2?????????+1???map
		DeconstructionListBo bo = policyDeconstructionService.gainMaterials(ids);
		// ????????????
		Analyse wonk = analyseAlgorithmService.policyWonk(bo, govPolicyDto.getFeatureNum());
//		System.out.println(wonk);

		// ????????????????????????
		PolicyAnalyse analyse = new PolicyAnalyse();
		analyse.setId(policyAnalyse.getId());
		if (wonk != null) {
			// ????????????
			boolean b = updatePolicyAndFeature(policyAnalyse.getId(), wonk, ids.size());
			if (b) {
				analyse.setAnalyseState(AnalyseConstant.TRUE);
			} else {
				analyse.setAnalyseState(AnalyseConstant.TWO);
			}
		} else {
			// ????????????
			analyse.setAnalyseState(AnalyseConstant.TWO);
		}
		// ??????????????????
		policyAnalyseService.updateById(analyse);
	}

	/**
	 * ?????????????????????????????????
	 *
	 * @param analyseId ????????????id
	 * @param wonk      ?????????????????????
	 * @param total     ???????????????
	 * @return
	 */
	private boolean updatePolicyAndFeature(Long analyseId, Analyse wonk, Integer total) {
		// ???????????????
		List<PolicyAnalyseFeature> list = spellFeatureData(analyseId, wonk);
		// ??????????????????
		PolicyAnalyse updatePolicy = new PolicyAnalyse();
		updatePolicy.setId(analyseId);
		updatePolicy.setAnalyseSummary(wonk.getSummary());
		updatePolicy.setPolicyNum(total);
		return policyAnalyseFeatureService.batchInsert(list) && policyAnalyseService.updateById(updatePolicy);
	}

	/**
	 * ?????????????????????
	 *
	 * @param analyseId ??????id
	 * @param wonk      ????????????
	 * @return
	 */
	private List<PolicyAnalyseFeature> spellFeatureData(Long analyseId, Analyse wonk) {
		List<PolicyAnalyseFeature> list = new ArrayList<>();
		for (Child child : wonk.getList()) {
			PolicyAnalyseFeature feature = new PolicyAnalyseFeature();
			feature.setFeatureNum(child.getFeatureNum());
			feature.setFeatureName(child.getFeatureName());
			feature.setAnalyseId(analyseId);
			list.add(feature);
		}
		return list;
	}
}
