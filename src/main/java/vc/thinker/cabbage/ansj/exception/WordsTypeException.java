package vc.thinker.cabbage.ansj.exception;

import vc.thinker.biz.exception.ServiceException;

/**
 * @Auther: HTH
 * @Date: 2018/7/21 17:20
 * @Description:
 */
public class WordsTypeException extends ServiceException {
    public WordsTypeException() {
        super();
    }

    public WordsTypeException(String message) {
        super(message);
    }
}
