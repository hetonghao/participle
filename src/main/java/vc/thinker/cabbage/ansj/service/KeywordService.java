package vc.thinker.cabbage.ansj.service;

import com.sinco.data.core.Page;
import com.sinco.dic.client.DicContent;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vc.thinker.biz.exception.ServiceException;
import vc.thinker.cabbage.ansj.bo.AnalysisKeywordBO;
import vc.thinker.cabbage.ansj.constant.WordsTypeConstants;
import vc.thinker.cabbage.ansj.constant.WordsTypeSaveConstants;
import vc.thinker.cabbage.ansj.util.PatternDateTimeUtil;
import vc.thinker.cabbage.ansj.util.PatternDateTimeUtil.PatternDate;
import vc.thinker.cabbage.gov.bo.EnterpriseBO;
import vc.thinker.cabbage.gov.bo.IndustryClassBO;
import vc.thinker.cabbage.gov.bo.KeywordBO;
import vc.thinker.cabbage.gov.dao.EnterpriseDao;
import vc.thinker.cabbage.gov.dao.IndustryClassDao;
import vc.thinker.cabbage.gov.dao.KeywordDao;
import vc.thinker.cabbage.gov.model.Keyword;
import vc.thinker.cabbage.gov.vo.KeywordVO;
import vc.thinker.sys.bo.DicAreaBO;
import vc.thinker.sys.dao.DicAreaDao;
import vc.thinker.sys.model.User;

import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * @Auther: HeTongHao
 * @Date: 2018/7/20 10:43
 * @Description: 关键字
 */
@Service
@Transactional(rollbackFor = {IllegalAccessException.class, ServiceException.class})
public class KeywordService extends ParticipleServiceBase {
    @Autowired
    private KeywordDao keywordDao;
    @Autowired
    private DicAreaDao dicAreaDao;
    @Autowired
    private IndustryClassDao industryClassDao;
    @Autowired
    private EnterpriseDao enterpriseDao;
    @Autowired
    private DicContent dicContent;

    /**
     * 刷新字典,添加或修改时可调用
     */
    @Override
    public void loadDic() {
        //导入关键字表
        List<KeywordBO> KeywordList = keywordDao.findAll();
        for (KeywordBO bo : KeywordList) {
            insertDic(bo.getName(), bo.getWordsType());
        }
        //导入甘肃地区词典
        DicAreaBO ganSu = dicAreaDao.findByName("甘肃省");
        insertDic("甘肃省", WordsTypeConstants.GS_NS);  //甘肃地区
        if (ganSu != null) {
            List<DicAreaBO> dicAreaList = dicContent.getDicsByParentCode(DicAreaBO.class, ganSu.getCode());
            for (DicAreaBO bo : dicAreaList) {
                insertDic(bo.getName(), WordsTypeConstants.GS_NS);  // 加入词典
            }
        }
    }

    /**
     * 保存
     *
     * @param keyword
     * @param user
     * @return
     */
    public KeywordBO save(Keyword keyword, User user) throws IllegalAccessException {
        Field[] fields = WordsTypeSaveConstants.class.getDeclaredFields();
        for (Field field : fields) {  //遍历所有的词性
            if ((field.get(WordsTypeConstants.class)).equals(keyword.getWordsType())) {
                keywordDao.save(keyword, user);
                super.insertDic(keyword.getName(), keyword.getWordsType());  //添加一个关键字给分词字典
                return keywordDao.findOne(keyword.getId());
            }
        }
        throw new IllegalArgumentException("无效的词性");
    }

    /**
     * 可扩展分页查询
     *
     * @param page
     * @param dynamicInfoVO
     * @return
     */
    public List<KeywordBO> pageByVO(Page<KeywordBO> page, KeywordVO dynamicInfoVO) {
        return keywordDao.pageByVO(page, dynamicInfoVO);
    }

    /**
     * 分析一段话，分析并拆分成对象列表
     *
     * @param text
     * @return
     */
    public List<AnalysisKeywordBO> analysisKeyword(String text) {
        List<AnalysisKeywordBO> analysisList = new ArrayList<>();  //返回list
        if (StringUtils.isNotBlank(text)) {  //一句话不为空
            analysisDate(text, analysisList);  //分析并提取时间
            analysisService(text, analysisList);  //分析并提取业务相关 关键字
        }
        return analysisList;
    }

    /**
     * 分析一句话,统一将业务相关的关键字装载到关键字列表
     *
     * @param text
     * @param analysisList
     */
    private void analysisService(final String text, List<AnalysisKeywordBO> analysisList) {
        String[] array = super.analysis(text);//分析文本 获得拆分的列表
        for (String temp : array) {  //遍历每组关键字
            String[] keywordGroup = temp.split("/");//获得一组关键字
            //封装bo
            AnalysisKeywordBO analysisKeywordBO;
            if (WordsTypeConstants.NT.equals(keywordGroup[1])) {  //企业
                analysisKeywordBO = new AnalysisKeywordBO();
                List<EnterpriseBO> entList = enterpriseDao.listLikeName(keywordGroup[0]);
                analysisKeywordBO.setCode(entList.isEmpty() ? null : entList.get(0).getCode());  //获得企业code
            } else if (WordsTypeConstants.GS_NS.equals(keywordGroup[1])) {  //地区
                analysisKeywordBO = new AnalysisKeywordBO();
                DicAreaBO dicAreaBO = dicAreaDao.findLikeName(keywordGroup[0]);
                analysisKeywordBO.setCode(dicAreaBO == null ? null : dicAreaBO.getCode());  //地区code
            } else if (WordsTypeConstants.IND.equals(keywordGroup[1])) {  //行业
                analysisKeywordBO = new AnalysisKeywordBO();
                List<IndustryClassBO> industryClassList = industryClassDao.listLikeName(keywordGroup[0]);
                analysisKeywordBO.setCode(industryClassList.isEmpty() ? null : String.valueOf(industryClassList.get(0).getId()));  //行业大类id
            } else if (WordsTypeConstants.V.equals(keywordGroup[1]) && "查看".equals(keywordGroup[0])) {  //查看
                analysisKeywordBO = new AnalysisKeywordBO();
            } else if (WordsTypeConstants.VN.equals(keywordGroup[1]) && "切换".equals(keywordGroup[0])) {  //切换
                analysisKeywordBO = new AnalysisKeywordBO();
            } else {
                continue;
            }
            analysisKeywordBO.setName(keywordGroup[0]);  //关键名
            analysisKeywordBO.setWordsType(keywordGroup[1]);  //词性
            analysisList.add(analysisKeywordBO);
        }
    }

    /**
     * 分析时间，将时间提取出来并且转换成时间戳,封装到对象列表
     *
     * @param text
     * @param analysisList
     */
    private void analysisDate(final String text, List<AnalysisKeywordBO> analysisList) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy年MM月dd日 HH时mm分ss秒");
        List<PatternDate> list = PatternDateTimeUtil.defaultAnalysis(text);  //调用默认分析策略
        Calendar calendar = Calendar.getInstance();
        for (PatternDate result : list) {
            if (result.isStarDateIs()) {  //开始时间
                analysisList.add(new AnalysisKeywordBO(result.getDate(), result.getText(), WordsTypeConstants.START_TIME));
            } else {//结束时间
                calendar.setTime(result.getDate());
                calendar.add(Calendar.MILLISECOND, -1);  //少一毫秒
                Date endDate = calendar.getTime();
                analysisList.add(new AnalysisKeywordBO(endDate, format.format(endDate), WordsTypeConstants.END_TIME));
            }
        }
    }

    /**
     * 删除一个
     * 受影响不为1，表示删除失败
     *
     * @param id
     * @return
     */
    public boolean delete(Long id) {
        KeywordBO keywordBO = keywordDao.findOne(id);
        if (keywordBO != null) {
            super.deleteDic(keywordBO.getName());  //删除一个关键字
            if (keywordDao.delete(id) == 1) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取一个
     *
     * @param id
     * @return
     */
    public KeywordBO findOne(Long id) {
        return keywordDao.findOne(id);
    }
}