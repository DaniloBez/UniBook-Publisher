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
}
