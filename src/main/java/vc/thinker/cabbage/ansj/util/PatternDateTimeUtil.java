package vc.thinker.cabbage.ansj.util;

import org.apache.commons.lang3.StringUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Auther: HTH
 * @Date: 2018/7/24 10:36
 * @Description: 分析时间帮助类
 */
public class PatternDateTimeUtil {
    private String text;
    private Matcher matcher;
    private List<PatternDate> patternDateList = new ArrayList<>();

    public PatternDateTimeUtil(String text) {
        this.text = text;
    }

    public static class PatternDate {
        /**
         * @param date 时间
         * @param text 时间的中文显示文本
         * @param text
         */
        public PatternDate(Date date, String text) {
            this.setDate(date);
            this.setText(text);
        }

        /**
         * @param date       时间
         * @param text       时间的中文显示文本
         * @param starDateIs 是否是开始时间
         */
        public PatternDate(Date date, String text, boolean starDateIs) {
            this.setDate(date);
            this.setText(text);
            this.setStarDateIs(starDateIs);
        }

        private Date date;
        private String text;
        private boolean starDateIs;

        public Date getDate() {
            return date;
        }

        public void setDate(Date date) {
            this.date = date;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public boolean isStarDateIs() {
            return starDateIs;
        }

        public void setStarDateIs(boolean starDateIs) {
            this.starDateIs = starDateIs;
        }
    }

    /**
     * 传入正则和转化格式，返回提取出的时间结果
     *
     * @param pattern
     * @return
     */
    public List<PatternDate> getDateList(String pattern) {
        matcher = Pattern.compile(pattern).matcher(text);//使用正则表达式提取结果
        if (matcher.find()) {
            do {
                SimpleDateFormat format = new SimpleDateFormat("yyyy年MM月dd日");
                StringBuilder parse = new StringBuilder();
                if (matcher.group().indexOf("号") != -1) {
                    parse.append(StringUtils.replace(matcher.group(), "号", "日")); //号同义为日
                } else if (matcher.group().indexOf("天") != -1) {
                    parse.append(StringUtils.replace(matcher.group(), "天", "日")); //天同义为日
                } else {
                    parse.append(matcher.group());
                }
                if (parse.toString().indexOf("本周") != -1) {
                    Date date = getWeekStartDate();
                    patternDateList.add(new PatternDate(date, format.format(date)));  //产生一个结果返回
                    return patternDateList;
                }
                //如果正则缺少月或年，补充当前年月
                if (pattern.indexOf("年") == -1) {
                    parse.insert(0, "本年");
                }
                if (pattern.indexOf("月") == -1) {
                    parse.insert(parse.indexOf("年") + 1, "空月");
                }
                if (pattern.indexOf("日") == -1) {
                    parse.append("空日");
                }
                //设置值
                String parseStr = parse.toString();
                if (parseStr.indexOf("本年") != -1) {
                    parseStr = StringUtils.replace(parseStr, "本年", Calendar.getInstance().get(Calendar.YEAR) + "年");
                } else if (parse.indexOf("今年") != -1) {
                    parseStr = StringUtils.replace(parseStr, "今年", Calendar.getInstance().get(Calendar.YEAR) + "年");
                }
                int quarterIndex = parseStr.indexOf("季度");
                if (quarterIndex != -1) {
                    Date dateStart;  //季度开始时间
                    if (parseStr.indexOf("本季度") != -1) {
                        dateStart = getCurrentQuarterStartDate();//季度开始时间
                        patternDateList.add(new PatternDate(dateStart, format.format(dateStart)));  //产生季度开始时间
                        return patternDateList;
                    } else {
                        Calendar c = Calendar.getInstance();
                        c.setTime(getYearStartDate());  //设定为今年的开始，月日时分秒归零
                        c.set(Calendar.YEAR, Integer.parseInt(parseStr.substring(0, 4)));  //设置年份
                        int quarter = Integer.parseInt(parseStr.substring(quarterIndex - 1, quarterIndex));  //获取季度数
                        c.set(Calendar.MONTH, (quarter - 1) * 3);
                        dateStart = c.getTime();  //季度开始时间
                        patternDateList.add(new PatternDate(dateStart, format.format(dateStart)));  //产生季度开始时间
                        continue;
                    }
                }
                if (parseStr.indexOf("空月") != -1) {
                    parseStr = StringUtils.replace(parseStr, "空月", 1 + "月");
                } else if (parseStr.indexOf("本月") != -1) {
                    parseStr = StringUtils.replace(parseStr, "本月", (Calendar.getInstance().get(Calendar.MONTH) + 1) + "月");
                }
                if (parseStr.indexOf("空日") != -1) {
                    parseStr = StringUtils.replace(parseStr, "空日", 1 + "日");
                } else if (parseStr.indexOf("本日") != -1) {
                    parseStr = StringUtils.replace(parseStr, "本日", Calendar.getInstance().get(Calendar.DATE) + "日");
                } else if (parse.indexOf("今日") != -1) {
                    parseStr = StringUtils.replace(parseStr, "今日", Calendar.getInstance().get(Calendar.DATE) + "日");
                }
                try {
                    Date date = format.parse(parseStr);
                    patternDateList.add(new PatternDate(date, format.format(date)));  //产生一个结果返回
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            } while (matcher.find());
        }
        return patternDateList;
    }

    private static final String patternYear = "([今|本]|\\d{4})[年]";
    private static final String patternDay = "([今|本]|[1-9]|([012]\\d)|(3[01]))[日|号|天]";
    private static final String patternMonth = "([本]|[1-9]|(1[0-2]))[月]";
    private static final String patternWeek = "[本]([周])";
    private static final String patternQuarter = "([本]|[1-4])季度";

    /**
     * 默认分析规则
     *
     * @param text 传入一句话，按默认的分析规则,标记每个时间是否是开始时间，并返回开始时间和结束时间对象
     * @return
     */
    public static List<PatternDate> defaultAnalysis(String text) {
        PatternDateTimeUtil dateUtil = new PatternDateTimeUtil(text);
        SimpleDateFormat format = new SimpleDateFormat("yyyy年MM月dd日");
        List<PatternDate> tempList;  //临时操作list
        List<PatternDate> boList = new ArrayList<>();  //用于返回最后结果list
        tempList = dateUtil.getDateList(patternWeek);
        //本周
        if (tempList.size() != 0) {
            Calendar a = Calendar.getInstance();
            boList.add(new PatternDate(tempList.get(0).getDate(), tempList.get(0).getText(), true));
            if (tempList.size() == 2) {  //如果提取出两个时间结果
                a.setTime(tempList.get(1).getDate());
            } else if (tempList.size() == 1) {  //如果提取出两个时间结果
                a.setTime(tempList.get(0).getDate());
            }
            a.add(Calendar.DAY_OF_WEEK_IN_MONTH, 1); //多加一个最下层单位(季+3月、月+1月、日+1日、年+1年、周+1周)
            boList.add(new PatternDate(a.getTime(), format.format(a.getTime()), false));//结束时间
            return boList;
        }
        //年-季度
        tempList = dateUtil.getDateList(patternYear + patternQuarter);
        if (tempList.size() != 0) {
            Calendar a = Calendar.getInstance();
            boList.add(new PatternDate(tempList.get(0).getDate(), tempList.get(0).getText(), true));
            if (tempList.size() == 2) {
                a.setTime(tempList.get(1).getDate());
            } else if (tempList.size() == 1) {
                tempList = dateUtil.getDateList(patternQuarter);  //再次拆分下层，应对 ：2018年1季度至3季度 这类情况
                a.setTime(tempList.get(0).getDate());
                if (tempList.size() == 3) {//最终查到3组，情况成立
                    int year = a.get(Calendar.YEAR);
                    a.setTime(tempList.get(2).getDate());
                    a.set(Calendar.YEAR, year);
                }
            }
            a.add(Calendar.MONTH, 3); //多加一个最下层单位(季+3月、月+1月、日+1日、年+1年)
            boList.add(new PatternDate(a.getTime(), format.format(a.getTime()), false));//结束时间
            return boList;
        }
        //null-季度
        tempList = dateUtil.getDateList(patternQuarter);
        if (tempList.size() != 0) {
            Calendar a = Calendar.getInstance();
            boList.add(new PatternDate(tempList.get(0).getDate(), tempList.get(0).getText(), true));
            if (tempList.size() == 2) {  //如果提取出两个时间结果
                a.setTime(tempList.get(1).getDate());
            } else if (tempList.size() == 1) {
                a.setTime(tempList.get(0).getDate());
            }
            a.add(Calendar.MONTH, 3);  //多加一个最下层单位(季+3月、月+1月、日+1日、年+1年)
            boList.add(new PatternDate(a.getTime(), format.format(a.getTime()), false));//结束时间
            return boList;
        }
        //年-月-日
        tempList = dateUtil.getDateList(patternYear + patternMonth + patternDay);
        if (tempList.size() != 0) {
            Calendar a = Calendar.getInstance();
            boList.add(new PatternDate(tempList.get(0).getDate(), tempList.get(0).getText(), true));
            if (tempList.size() == 2) {  //如果提取出两个时间结果
                a.setTime(tempList.get(1).getDate());
            } else if (tempList.size() == 1) {  //如果提取出两个时间结果
                tempList = dateUtil.getDateList(patternDay);  //再次拆分下层，应对 ：2018年4月4日至14日 这类情况
                a.setTime(tempList.get(0).getDate());
                if (tempList.size() == 3) {   //最终查到3组，情况成立
                    int year = a.get(Calendar.YEAR);
                    int month = a.get(Calendar.MONTH);
                    a.setTime(tempList.get(2).getDate());
                    a.set(Calendar.YEAR, year);
                    a.set(Calendar.MONTH, month);
                }
            }
            a.add(Calendar.DATE, 1); //多加一个最下层单位(季+3月、月+1月、日+1日、年+1年)
            boList.add(new PatternDate(a.getTime(), format.format(a.getTime()), false));//结束时间
            return boList;
        }
        //年-月-null
        tempList = dateUtil.getDateList(patternYear + patternMonth);
        if (tempList.size() != 0) {
            Calendar a = Calendar.getInstance();
            boList.add(new PatternDate(tempList.get(0).getDate(), tempList.get(0).getText(), true));
            if (tempList.size() == 2) {  //如果提取出两个时间结果
                a.setTime(tempList.get(1).getDate());
            } else if (tempList.size() == 1) {
                a.setTime(tempList.get(0).getDate());
                tempList = dateUtil.getDateList(patternMonth);  //再次拆分下层，应对 ：2018年4月至8月 这类情况
                if (tempList.size() == 3) {   //最终查到3组，情况成立
                    int year = a.get(Calendar.YEAR);
                    a.setTime(tempList.get(2).getDate());
                    a.set(Calendar.YEAR, year);
                }
            }
            a.add(Calendar.MONTH, 1);  //多加一个最下层单位(季+3月、月+1月、日+1日、年+1年)
            boList.add(new PatternDate(a.getTime(), format.format(a.getTime()), false));//结束时间
            return boList;
        }
        //null-月-日
        tempList = dateUtil.getDateList(patternMonth + patternDay);
        if (tempList.size() != 0) {
            Calendar a = Calendar.getInstance();
            boList.add(new PatternDate(tempList.get(0).getDate(), tempList.get(0).getText(), true));
            if (tempList.size() == 2) {  //如果提取出两个时间结果
                a.setTime(tempList.get(1).getDate());
            } else if (tempList.size() == 1) {
                a.setTime(tempList.get(0).getDate());
                tempList = dateUtil.getDateList(patternDay);  //再次拆分下层，应对 ：2018年4月至8月 这类情况
                if (tempList.size() == 3) {   //最终查到3组，情况成立
                    int year = a.get(Calendar.YEAR);
                    int month = a.get(Calendar.MONTH);
                    a.setTime(tempList.get(2).getDate());
                    a.set(Calendar.MONTH, month);
                    a.set(Calendar.YEAR, year);
                }
            }
            a.add(Calendar.DATE, 1);  //多加一个最下层单位(季+3月、月+1月、日+1日、年+1年)
            boList.add(new PatternDate(a.getTime(), format.format(a.getTime()), false));//结束时间
            return boList;
        }
        //年-null-null
        tempList = dateUtil.getDateList(patternYear);
        if (tempList.size() != 0) {
            Calendar a = Calendar.getInstance();
            boList.add(new PatternDate(tempList.get(0).getDate(), tempList.get(0).getText(), true));
            if (tempList.size() == 2) {  //如果提取出两个时间结果
                a.setTime(tempList.get(1).getDate());
            } else if (tempList.size() == 1) {
                a.setTime(tempList.get(0).getDate());
            }
            a.add(Calendar.YEAR, 1);  //多加一个最下层单位(季+3月、月+1月、日+1日、年+1年)
            boList.add(new PatternDate(a.getTime(), format.format(a.getTime()), false));//结束时间
            return boList;
        }
        //null-月-null
        tempList = dateUtil.getDateList(patternMonth);
        if (tempList.size() != 0) {
            Calendar a = Calendar.getInstance();
            boList.add(new PatternDate(tempList.get(0).getDate(), tempList.get(0).getText(), true));
            if (tempList.size() == 2) {  //如果提取出两个时间结果
                a.setTime(tempList.get(1).getDate());
            } else if (tempList.size() == 1) {
                a.setTime(tempList.get(0).getDate());
            }
            a.add(Calendar.MONTH, 1);  //多加一个最下层单位(季+3月、月+1月、日+1日、年+1年)
            boList.add(new PatternDate(a.getTime(), format.format(a.getTime()), false));//结束时间
            return boList;
        }
        //null-null-日
        tempList = dateUtil.getDateList(patternDay);
        if (tempList.size() != 0) {
            Calendar a = Calendar.getInstance();
            boList.add(new PatternDate(tempList.get(0).getDate(), tempList.get(0).getText(), true));
            if (tempList.size() == 2) {  //如果提取出两个时间结果
                a.setTime(tempList.get(1).getDate());
            } else if (tempList.size() == 1) {
                a.setTime(tempList.get(0).getDate());
            }
            a.add(Calendar.DATE, 1);  //多加一个最下层单位(季+3月、月+1月、日+1日、年+1年)
            boList.add(new PatternDate(a.getTime(), format.format(a.getTime()), false));//结束时间
            return boList;
        }
        return boList;
    }

    /**
     * 获得本周第一天
     *
     * @return
     */
    public static Date getWeekStartDate() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    /**
     * 获得下周第一天
     *
     * @return
     */
    public static Date getNextWeekStartDate() {
        Calendar cal = Calendar.getInstance();
        cal.setTime(getWeekStartDate());
        cal.add(Calendar.DAY_OF_WEEK_IN_MONTH, 1); //加一周
        return cal.getTime();
    }

    /**
     * 获得本年第一天
     *
     * @return date
     */
    public static Date getYearStartDate() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.MONTH, 0);
        cal.set(Calendar.DATE, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    /**
     * 获得本年的偏移季度,0为第一季度,-1为去年第四季度，4为明年第一季度
     *
     * @param offsetQuarter 偏移季度
     * @return
     */
    public static Date getOffsetQuarterStartDate(int offsetQuarter) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(getYearStartDate());
        cal.set(Calendar.MONTH, offsetQuarter * 3);
        return cal.getTime();
    }

    /**
     * 获得当前季度
     *
     * @return date
     */
    public static Date getCurrentQuarterStartDate() {
        Calendar cal = Calendar.getInstance();
        int month = cal.get(Calendar.MONTH);  //当前月
        if (month <= 2) {
            return getOffsetQuarterStartDate(0);  //1季度
        } else if (month <= 5) {
            return getOffsetQuarterStartDate(1);  //2季度
        } else if (month <= 8) {
            return getOffsetQuarterStartDate(2);
        } else if (month <= 11) {
            return getOffsetQuarterStartDate(3);
        }
        return cal.getTime();
    }
}
