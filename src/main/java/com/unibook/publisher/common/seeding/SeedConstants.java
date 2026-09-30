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

    //chapter (по 3 розділи на активні рукописи)
    public static final UUID CHAPTER_IN_PROGRESS_1_ID = UUID.fromString("55555555-5555-5555-5555-555555555551");
    public static final UUID CHAPTER_IN_PROGRESS_2_ID = UUID.fromString("55555555-5555-5555-5555-555555555552");
    public static final UUID CHAPTER_IN_PROGRESS_3_ID = UUID.fromString("55555555-5555-5555-5555-555555555553");
    public static final UUID CHAPTER_TEXT_APPROVED_1_ID = UUID.fromString("55555555-5555-5555-5555-555555555554");
    public static final UUID CHAPTER_TEXT_APPROVED_2_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    public static final UUID CHAPTER_TEXT_APPROVED_3_ID = UUID.fromString("55555555-5555-5555-5555-555555555556");
    public static final UUID CHAPTER_IN_DESIGN_1_ID = UUID.fromString("55555555-5555-5555-5555-555555555557");
    public static final UUID CHAPTER_IN_DESIGN_2_ID = UUID.fromString("55555555-5555-5555-5555-555555555558");
    public static final UUID CHAPTER_IN_DESIGN_3_ID = UUID.fromString("55555555-5555-5555-5555-555555555559");
    public static final UUID CHAPTER_PUBLISHED_1_ID = UUID.fromString("55555555-5555-5555-5555-55555555555a");
    public static final UUID CHAPTER_PUBLISHED_2_ID = UUID.fromString("55555555-5555-5555-5555-55555555555b");
    public static final UUID CHAPTER_PUBLISHED_3_ID = UUID.fromString("55555555-5555-5555-5555-55555555555c");

    //revision
    public static final UUID REVISION_IN_PROGRESS_1_V1_ID = UUID.fromString("66666666-6666-6666-6666-666666666661");
    public static final UUID REVISION_IN_PROGRESS_1_V2_ID = UUID.fromString("66666666-6666-6666-6666-666666666662");
    public static final UUID REVISION_IN_PROGRESS_1_V3_ID = UUID.fromString("66666666-6666-6666-6666-666666666663");
    public static final UUID REVISION_IN_PROGRESS_2_V1_ID = UUID.fromString("66666666-6666-6666-6666-666666666664");

    //feedback thread
    public static final UUID THREAD_SUGGESTION_OPEN_ID = UUID.fromString("77777777-7777-7777-7777-777777777771");
    public static final UUID THREAD_DISCUSSION_RESOLVED_ID = UUID.fromString("77777777-7777-7777-7777-777777777772");

    //thread message
    public static final UUID MESSAGE_SUGGESTION_1_ID = UUID.fromString("88888888-8888-8888-8888-888888888881");
    public static final UUID MESSAGE_DISCUSSION_1_ID = UUID.fromString("88888888-8888-8888-8888-888888888882");
    public static final UUID MESSAGE_DISCUSSION_2_ID = UUID.fromString("88888888-8888-8888-8888-888888888883");

    //cover version
    public static final UUID COVER_IN_DESIGN_V1_ID = UUID.fromString("99999999-9999-9999-9999-999999999991");
    public static final UUID COVER_PUBLISHED_V1_ID = UUID.fromString("99999999-9999-9999-9999-999999999992");
}
