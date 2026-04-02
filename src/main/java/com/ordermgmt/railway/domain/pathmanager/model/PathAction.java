package com.ordermgmt.railway.domain.pathmanager.model;

/** Allowed user and IM actions in the TTT path request lifecycle. */
public enum PathAction {
    SEND_REQUEST,
    MODIFY_REQUEST,
    WITHDRAW,
    IM_RECEIPT,
    IM_DRAFT_OFFER,
    IM_NO_ALTERNATIVE,
    IM_ERROR,
    REJECT_WITH_REVISION,
    REJECT_WITHOUT_REVISION,
    IM_FINAL_OFFER,
    ACCEPT_OFFER,
    IM_BOOK,
    REQUEST_MODIFICATION,
    CANCEL_PATH,
    IM_ANNOUNCE_ALTERATION,
    IM_ALTERATION_OFFER,
    ACCEPT_ALTERATION,
    REJECT_ALTERATION
}
