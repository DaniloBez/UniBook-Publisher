package com.unibook.publisher.common.exception.conflict;

public class StaffAlreadyAssignedException extends DuplicateResourceException {
    public StaffAlreadyAssignedException(Object role) {
        super(String.format("Співробітника на роль %s вже призначено", role));
    }
}
