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
        List<EnterpriseBO> enterpriseBOList = enterpriseDao.findAll();  //所有企业
        for (EnterpriseBO enterpriseBO : enterpriseBOList) {
            insertDic(enterpriseBO.getName(), WordsTypeConstants.NT);  //企业全名
            insertDic(enterpriseBO.getAbbreviation(), WordsTypeConstants.NT);  //企业简称
        }
        List<IndustryClassBO> industryClassBOList = industryClassDao.findAll();  //所有行业
        for (IndustryClassBO industryClassBO : industryClassBOList) {
            insertDic(industryClassBO.getName(), WordsTypeConstants.IND);  //行业名称
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
        Class constants = WordsTypeSaveConstants.class;
        Field[] fields = constants.getDeclaredFields();
        for (Field field : fields) {  //遍历所有的词性
            if ((field.get(constants)).equals(keyword.getWordsType())) {
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
        Date startTime = null;
        for (PatternDate result : list) {
            if (result.isStarDateIs()) {  //开始时间
                startTime = result.getDate();
                analysisList.add(new AnalysisKeywordBO(result.getDate(), result.getText(), WordsTypeConstants.START_TIME));
            } else {//结束时间
                AnalysisKeywordBO unit = new AnalysisKeywordBO();
                unit.setName(getUnitWhole(startTime, result.getDate()));//分析一个时间段是否是一个单位整
                unit.setWordsType("unit");
                analysisList.add(unit);
                calendar.setTime(result.getDate());
                calendar.add(Calendar.MILLISECOND, -1);  //少一毫秒
                Date endDate;
                endDate = dateSpilloverTreatment(calendar.getTime(), 1000L * 60L * 5L);//时间溢出处理，超过今天的时间替换成当前时间,允许5分钟间隔不被替换
                analysisList.add(new AnalysisKeywordBO(endDate, format.format(endDate), WordsTypeConstants.END_TIME));
            }
        }
    }

    /**
     * 时间溢出处理，超过今天的时间替换成当前时间
     *
     * @param time
     * @param ms   允许间隔在毫秒数之内不被替换
     * @return
     */
    private Date dateSpilloverTreatment(Date time, Long ms) {
        if (time == null) {
            return time;
        }
        if (ms == null) {
            ms = 0L;
        }
        Date currentDate = new Date();
        if (time.getTime() > currentDate.getTime() + ms) {//时间比当前时间
            time = currentDate;
        }
        return time;
    }

    /**
     * 分析一个时间段是否是一个单位整
     *
     * @param startTime
     * @param endTime
     * @return
     */
    private String getUnitWhole(Date startTime, Date endTime) {
        Calendar startCal = Calendar.getInstance();
        startCal.setTime(startTime);
        int day = (int) ((endTime.getTime() - startTime.getTime()) / (1000 * 3600 * 24));  //相差天数
        if (day > 360) {
            startCal.add(Calendar.YEAR, 1);
            if (startCal.getTime().getTime() == endTime.getTime() && startCal.get(Calendar.MONTH) == 0) {
                return "年";
            }
        } else if (day > 84) {
            startCal.add(Calendar.MONTH, 3);
            int month = startCal.get(Calendar.MONTH);
            if (startCal.getTime().getTime() == endTime.getTime() && (month == 0 || month == 3 || month == 6 || month == 9 || month == 12)) {
                return "季";
            }
        } else if (day >= 28) {
            startCal.add(Calendar.MONTH, 1);
            if (startCal.getTime().getTime() == endTime.getTime() && startCal.get(Calendar.DATE) == 1) {
                return "月";
            }
        } else if (day == 7) {
            startCal.add(Calendar.DAY_OF_WEEK_IN_MONTH, 1);
            if (startCal.getTime().getTime() == endTime.getTime() && startCal.get(Calendar.DAY_OF_WEEK) == 2) {//周一
                return "周";
            }
        } else if (day == 1) {
            startCal.add(Calendar.DATE, 1);
            if (startCal.getTime().getTime() == endTime.getTime()) {
                return "日";
            }
        }
        startCal.setTime(startTime);  //重置时间
        if (startCal.get(Calendar.DATE) == 1) {
            startCal.add(Calendar.MONTH, day / 28);  //间隔几个月
            if (startCal.getTime().getTime() == endTime.getTime()) {
                return "跨月";
            }
        }
        return null;
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