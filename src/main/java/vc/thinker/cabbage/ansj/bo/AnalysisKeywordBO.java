package vc.thinker.cabbage.ansj.bo;

import java.util.Date;

/**
 * @Auther: HTH
 * @Date: 2018/7/20 17:02
 * @Description:
 */
public class AnalysisKeywordBO extends AnalysisResult {
    public AnalysisKeywordBO() {
    }

    public AnalysisKeywordBO(Date date, String name, String wordsType) {
        this.setDate(date);
        super.setName(name);
        super.setWordsType(wordsType);
    }

    /**
     * code
     */
    private String code;
    /**
     * 时间
     */
    private Date date;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }
}
