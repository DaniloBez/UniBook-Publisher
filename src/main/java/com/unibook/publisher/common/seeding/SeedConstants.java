package com.unibook.publisher.common.seeding;

import java.util.UUID;

public final class SeedConstants {
    private SeedConstants() {}

    public static final String DEFAULT_PASSWORD = "SuperSecretPassword123!";

    public static final UUID ADMIN_ID        = UUID.fromString("00000000-0000-0000-0000-000000000001");
    public static final UUID CHIEF_EDITOR_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    public static final UUID EDITOR_ID       = UUID.fromString("00000000-0000-0000-0000-000000000003");
    public static final UUID DESIGNER_ID     = UUID.fromString("00000000-0000-0000-0000-000000000004");
    public static final UUID AUTHOR_1_ID     = UUID.fromString("00000000-0000-0000-0000-000000000005");
    public static final UUID AUTHOR_2_ID     = UUID.fromString("00000000-0000-0000-0000-000000000006");
    public static final UUID ACCOUNTANT_ID   = UUID.fromString("00000000-0000-0000-0000-000000000007");

    //genre
    public static final UUID GENRE_FANTASY_ID =             UUID.fromString("11111111-1111-1111-1111-111111111111");
    public static final UUID GENRE_MYSTERY_ID  =            UUID.fromString("11111111-1111-1111-1111-111111111112");
    public static final UUID GENRE_NONFICTION_ID =          UUID.fromString("11111111-1111-1111-1111-111111111113");
    public static final UUID GENRE_FICTION_ID =             UUID.fromString("11111111-1111-1111-1111-111111111114");
    public static final UUID GENRE_THRILLER_ID =            UUID.fromString("11111111-1111-1111-1111-111111111116");
    public static final UUID GENRE_ROMANCE_ID =             UUID.fromString("11111111-1111-1111-1111-111111111117");
    public static final UUID GENRE_HISTORICAL_FICTION_ID =  UUID.fromString("11111111-1111-1111-1111-111111111118");

    //manuscript (different status)
    public static final UUID MANUSCRIPT_SUBMITTED_ID =      UUID.fromString("22222222-2222-2222-2222-222222222221");
    public static final UUID MANUSCRIPT_IN_PROGRESS_ID =    UUID.fromString("22222222-2222-2222-2222-222222222222");
    public static final UUID MANUSCRIPT_TEXT_APPROVED_ID =  UUID.fromString("22222222-2222-2222-2222-222222222223");
    public static final UUID MANUSCRIPT_IN_DESIGN_ID =      UUID.fromString("22222222-2222-2222-2222-222222222224");
    public static final UUID MANUSCRIPT_REJECTED_ID =       UUID.fromString("22222222-2222-2222-2222-222222222225");
    public static final UUID MANUSCRIPT_PUBLISHED_ID =      UUID.fromString("22222222-2222-2222-2222-222222222226");
    public static final UUID MANUSCRIPT_POSTPONED_ID =      UUID.fromString("22222222-2222-2222-2222-222222222227");

    //team assignment
    public static final UUID ASSIGNMENT_EDITOR_SUBMITTED_ID =   UUID.fromString("33333333-3333-3333-3333-333333333331");
    public static final UUID ASSIGNMENT_DESIGNER_IN_DESIGN_ID = UUID.fromString("33333333-3333-3333-3333-333333333332");

    //contract
    public static final UUID CONTRACT_DRAFT_ID = UUID.fromString("44444444-4444-4444-4444-444444444441");
    public static final UUID CONTRACT_ACTIVE_ID = UUID.fromString("44444444-4444-4444-4444-444444444442");
    public static final UUID CONTRACT_TERMINATED_ID = UUID.fromString("44444444-4444-4444-4444-444444444443");
}
