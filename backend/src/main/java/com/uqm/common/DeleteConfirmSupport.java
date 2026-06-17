package com.uqm.common;

public final class DeleteConfirmSupport {

    public static final String CONFIRM_PHRASE = "我确认删除";

    private DeleteConfirmSupport() {
    }

    public static void validate(String confirmPhrase) {
        if (!CONFIRM_PHRASE.equals(confirmPhrase)) {
            throw new BusinessException(400, "请输入「" + CONFIRM_PHRASE + "」以确认删除");
        }
    }
}
