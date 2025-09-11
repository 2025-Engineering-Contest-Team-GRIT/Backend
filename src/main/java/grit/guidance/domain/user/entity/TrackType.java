package grit.guidance.domain.user.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TrackType {
    PRIMARY("1트랙"),
    SECONDARY("2트랙");

    private final String description;
}