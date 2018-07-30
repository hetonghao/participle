package vc.thinker.cabbage.ansj.service;

import org.ansj.domain.Result;
import org.ansj.library.DicLibrary;
import org.ansj.splitWord.analysis.DicAnalysis;
import org.apache.commons.lang3.StringUtils;
import org.nlpcn.commons.lang.tire.domain.Forest;

import javax.annotation.PostConstruct;

/**
 * @Auther: HTH
 * @Date: 2018/7/21 14:27
 * @Description: 分词超类
 */
public abstract class ParticipleServiceBase {
    /**
     * key
     */
    protected static final String key = "dic_mykey";

    /**
     * 初始化，加载数据关键字字典
     */
    @PostConstruct
    private void init() {
        DicLibrary.put(key, key, new Forest());  //加入自定义key
        loadDic();  //加载字典
        DicAnalysis.parse("xx", DicLibrary.gets(key)); //加载类时，执行一次分析可以提高后续分析速度
    }


    /**
     * 添加一个关键字到词库
     *
     * @param keyword   关键字
     * @param wordsType 标记词性
     */
    protected void insertDic(String keyword, String wordsType) {
        DicLibrary.insert(key, keyword, wordsType, 1000);
    }

    /**
     * 删除一个关键字
     *
     * @param keyword
     */
    protected void deleteDic(String keyword) {
        DicLibrary.delete(key, keyword);
    }

    /**
     * 刷新字典,添加或修改时可调用
     */
    abstract void loadDic();

    /**
     * 分析文本
     *
     * @param text
     * @return 返回string数组，每个对象为   '关键字/词性'
     */
    protected String[] analysis(String text) {
        Result result = DicAnalysis.parse(StringUtils.remove(text, " "), DicLibrary.gets(key));  //去空格再分析
        return StringUtils.join(result).split(",");
    }
}
