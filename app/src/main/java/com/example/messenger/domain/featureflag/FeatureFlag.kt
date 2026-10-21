package com.example.messenger.domain.featureflag

enum class FeatureFlag(val key: String, val parent: FeatureFlag? = null) {
    CALLS("calls"),
    CALLS_VIDEO("calls_video", CALLS),
    CALLS_FLIP_CAMERA("calls_flip_camera", CALLS),
    CALLS_SPEAKER("calls_speaker", CALLS),
    CALLS_PIP("calls_pip", CALLS),
    CALLS_FLOATING_BUBBLE("calls_floating_bubble", CALLS),

    VOICE_MESSAGES("voice_messages"),
    VIDEO_NOTES("video_notes"),
    RECORDING_LOCK("recording_lock"),

    CLIP_PLAYBACK("clip_playback"),
    CLIP_SEEK("clip_seek", CLIP_PLAYBACK),
    CLIP_MINI_PLAYER("clip_mini_player", CLIP_PLAYBACK),
    CLIP_PLAYBACK_SPEED("clip_playback_speed", CLIP_MINI_PLAYER),

    MEDIA_SENDING("media_sending"),
    MEDIA_CAMERA("media_camera", MEDIA_SENDING),
    MEDIA_PICKER_FILTERS("media_picker_filters", MEDIA_SENDING),
    MEDIA_VIEWER("media_viewer"),
    MEDIA_DOWNLOAD("media_download"),
    MEDIA_CANCEL_TRANSFER("media_cancel_transfer"),

    MESSAGE_MENU("message_menu"),
    MESSAGE_COPY("message_copy", MESSAGE_MENU),
    MESSAGE_DELETE_FOR_ME("message_delete_for_me", MESSAGE_MENU),
    MESSAGE_DELETE_FOR_EVERYONE("message_delete_for_everyone", MESSAGE_MENU),
    MESSAGE_REPLY("message_reply"),
    MESSAGE_SWIPE_REPLY("message_swipe_reply", MESSAGE_REPLY),
    MESSAGE_REPLY_JUMP("message_reply_jump", MESSAGE_REPLY),
    CHAT_SWIPE_NAVIGATION("chat_swipe_navigation"),

    CHAT_LIST_SWIPE_ACTIONS("chat_list_swipe_actions"),
    CHAT_LIST_MARK_READ("chat_list_mark_read", CHAT_LIST_SWIPE_ACTIONS),
    CHAT_LIST_DELETE("chat_list_delete", CHAT_LIST_SWIPE_ACTIONS),
    CHAT_LIST_DELETE_FOR_EVERYONE("chat_list_delete_for_everyone", CHAT_LIST_SWIPE_ACTIONS),
    CHAT_LIST_EDIT("chat_list_edit", CHAT_LIST_SWIPE_ACTIONS),
    CHAT_LIST_PULL_REFRESH("chat_list_pull_refresh"),

    SEARCH("search"),
    SEARCH_RECENT("search_recent", SEARCH),

    PROFILE_AVATAR_UPLOAD("profile_avatar_upload"),
    PROFILE_AVATAR_CAMERA("profile_avatar_camera", PROFILE_AVATAR_UPLOAD),
    PROFILE_EDIT("profile_edit"),
    PROFILE_CHANGE_PHONE("profile_change_phone"),
    PROFILE_EDIT_CHANGE_PHOTO("profile_edit_change_photo"),
    ACCOUNTS_SWITCH("accounts_switch"),

    PARTNER_PROFILE_EDIT_CONTACT("partner_profile_edit_contact"),
    PARTNER_PROFILE_MUTE("partner_profile_mute"),
    PARTNER_PROFILE_BLOCK("partner_profile_block"),
    PARTNER_PROFILE_SHARED_MEDIA("partner_profile_shared_media"),

    APPEARANCE_WALLPAPERS("appearance_wallpapers"),
    APPEARANCE_CUSTOM_WALLPAPER("appearance_custom_wallpaper", APPEARANCE_WALLPAPERS),
    APPEARANCE_WALLPAPER_INTENSITY("appearance_wallpaper_intensity", APPEARANCE_WALLPAPERS),
    APPEARANCE_DARK_THEME("appearance_dark_theme"),
    APPEARANCE_BAR_POSITION("appearance_bar_position"),
    APPEARANCE_TEXT_SIZE("appearance_text_size"),
    APPEARANCE_FONT("appearance_font"),

    SETTINGS_PRIVACY_CONTROLS("settings_privacy_controls"),
    SETTINGS_SECURITY("settings_security"),
    SETTINGS_NOTIFICATION_CONTROLS("settings_notification_controls"),
    SETTINGS_DATA_STORAGE("settings_data_storage"),
    SETTINGS_LANGUAGE("settings_language"),

    NOTIFICATION_INLINE_REPLY("notification_inline_reply"),
    NOTIFICATION_MARK_READ("notification_mark_read"),
    NOTIFICATION_SHORTCUTS("notification_shortcuts"),
    NOTIFICATION_BUBBLES("notification_bubbles", NOTIFICATION_SHORTCUTS);

    companion object {
        fun fromKey(key: String): FeatureFlag? = entries.firstOrNull { it.key == key }

        fun splitKeys(csv: String): List<String> =
            csv.split(',').map { it.trim() }.filter { it.isNotEmpty() }

        fun parseKeys(csv: String): Set<FeatureFlag> =
            splitKeys(csv).mapNotNull { fromKey(it) }.toSet()

        fun unknownKeys(csv: String): List<String> =
            splitKeys(csv).filter { fromKey(it) == null }
    }
}
