package info.plateaukao.einkbro.util

class Constants {
    companion object {
        const val DEFAULT_HOME_URL = "https://www.google.com/"
        const val MIME_TYPE_EPUB = "application/epub+zip"
        const val MIME_TYPE_PDF = "application/pdf"
        const val MIME_TYPE_TEXT = "text/plain"
        const val MIME_TYPE_ANY = "*/*"
        const val MIME_TYPE_FONT = "application/x-font-ttf"
        // from https://github.com/Smile4ever/Neat-URL

        const val NEAT_URL_DATA = """
            {
    "categories": [
        { "name": "Action Map", "params": ["action_object_map", "action_ref_map", "action_type_map"]},
        { "name": "AliExpress.com", "params": ["aff_platform", "aff_trace_key", "algo_expid@*.aliexpress.*", "algo_pvid@*.aliexpress.com", "btsid@*.aliexpress.com", "expid@*.aliexpress.com", "initiative_id@*.aliexpress.com", "scm_id@*.aliexpress.com", "spm@*.aliexpress.com", "ws_ab_test*.aliexpress.com"]},
        { "name": "Amazon", "params": ["_encoding@amazon.*", "ascsubtag@amazon.*", "pd_rd_*@amazon.*", "pf@amazon.*", "pf_rd_*@amazon.*", "psc@amazon.*", "ref_@amazon.*", "tag@amazon.*"]},
        { "name": "Bilibili.com", "params": ["callback@bilibili.com"]},
        { "name": "Bing", "params": ["cvid@bing.com", "form@bing.com", "pq@bing.com", "qs@bing.com", "sc@bing.com", "sk@bing.com", "sp@bing.com"]},
        { "name": "Campaign tracking (Adobe Analytics)", "params": ["sc_cid"]},
        { "name": "Campaign tracking (Adobe Marketo)", "params": ["mkt_tok"]},
        { "name": "Campaign tracking (Amazon Kendra)", "params": ["trk", "trkCampaign"]},
        { "name": "Campaign tracking (at)", "params": ["at_campaign", "at_custom*", "at_medium"]},
        { "name": "Campaign tracking (Change.org)", "params": ["guest@change.org", "recruited_by_id@change.org", "recruiter@change.org", "short_display_name@change.org", "source_location@change.org"]},
        { "name": "Campaign tracking (DPG Media)", "params": ["dpg_*"]},
        { "name": "Campaign tracking (Google Analytics, ga)", "params": ["ga_*", "gclid", "gclsrc"]},
        { "name": "Campaign tracking (Humble Bundle)", "params": ["hmb_campaign", "hmb_medium", "hmb_source"]},
        { "name": "Campaign tracking (IBM Acoustic Campaign)", "params": ["spJobID", "spMailingID", "spReportId", "spUserID"]},
        { "name": "Campaign tracking (itm)", "params": ["itm_*"], "docs": "https://www.parse.ly/help/post/4843/campaign-data-tracking/"},
        { "name": "Campaign tracking (Omniture)", "params": ["s_cid"], "docs": "https://moz.com/community/q/omniture-tracking-code-urls-creating-duplicate-content"},
        { "name": "Campaign tracking (Oracle Eloqua)", "params": ["assetId", "assetType", "campaignId", "elqTrack", "elqTrackId", "recipientId", "siteId"]},
        { "name": "Campaign tracking (MailChimp)", "params": ["mc_cid", "mc_eid"], "docs": "https://www.learndigitaladvertising.com/solved-why-how-to-remove-mc_cid-and-mc_eid-from-google-analytics/"},
        { "name": "Campaign tracking (Matomo/Piwik)", "params": ["mtm_*", "pk_*"]},
        { "name": "Campaign tracking (ns)", "params": ["ns_*"]},
        { "name": "Campaign tracking (sc)", "params": ["sc_campaign", "sc_channel", "sc_content", "sc_country", "sc_geo", "sc_medium", "sc_outcome"]},
        { "name": "Campaign tracking (stm)", "params": ["stm_*"]},
        { "name": "Campaign tracking (utm)", "params": ["nr_email_referer", "utm_*"]},
        { "name": "Campaign tracking (Vero)", "params": ["vero_conv", "vero_id"], "docs": "https://help.getvero.com/articles/conversion-tracking.html"},
        { "name": "Campaign tracking (Yandex)", "params": ["_openstat", "yclid"], "docs": "https://yandex.com/support/direct/statistics/url-tags.html"},
        { "name": "Campaign tracking (others)", "params": ["c_id", "campaign_id", "Campaign", "cmpid", "mbid", "ncid"], "docs": "https://www.parse.ly/help/post/4843/campaign-data-tracking/"},
        { "name": "Caseking.de", "params": ["campaign@caseking.de", "sPartner@caseking.de"]},
        { "name": "Ebay", "params": ["hash@ebay.*", "_trkparms@ebay.*", "_trksid@ebay.*", "amdata@ebay.*", "epid@ebay.*", "hash@ebay.*", "var@ebay.*"]},
        { "name": "Etsy", "params": ["click_key@etsy.com", "click_sum@etsy.com", "organic_search_click@etsy.com", "ref@etsy.com"]},
        { "name": "Facebook", "params": ["fb_action_ids", "fb_action_types", "fb_ref", "fb_source", "fbclid", "hrc@facebook.com", "refsrc@facebook.com"]},
        { "name": "Google", "params": ["ei@google.*", "gs_gbg@google.*", "gs_l", "gs_lcp@google.*", "gs_mss@google.*", "gs_rn@google.*", "gws_rd@google.*", "sei@google.*", "ved@google.*"]},
        { "name": "Hubspot", "params": ["_hsenc", "_hsmi", "__hssc", "__hstc", "hsCtaTracking"]},
        { "name": "IMDb", "params": ["pf_rd_*@imdb.com", "ref_@imdb.com"]},
        { "name": "LinkedIn", "params": ["eBP@linkedin.com", "lgCta@linkedin.com", "lgTemp@linkedin.com", "lipi@linkedin.com", "midSig@linkedin.com", "midToken@linkedin.com", "recommendedFlavor@linkedin.com", "refId@linkedin.com", "trackingId@linkedin.com", "trk@linkedin.com", "trkEmail@linkedin.com"]},
        { "name": "Medium", "params": ["_branch_match_id@medium.com", "source@medium.com"]},
        { "name": "SourceForge.net", "params": ["position@sourceforge.net", "source@sourceforge.net"]},
        { "name": "Spotify", "params": ["context@open.spotify.com", "si@open.spotify.com"]},
        { "name": "TikTok", "params": ["_d@tiktok.com", "checksum@tiktok.com", "is_copy_url@tiktok.com", "is_from_webapp@tiktok.com", "language@tiktok.com", "preview_pb@tiktok.com", "sec_user_id@tiktok.com", "sender_device@tiktok.com", "sender_web_id@tiktok.com", "share_app_id@tiktok.com", "share_link_id@tiktok.com", "share_item_id@tiktok.com", "source@tiktok.com", "timestamp@tiktok.com", "tt_from@tiktok.com", "u_code@tiktok.com", "user_id@tiktok.com"]},
        { "name": "Twitch.tv", "params": ["tt_content", "tt_medium"]},
        { "name": "Twitter", "params": ["cxt@*.twitter.com", "ref_*@*.twitter.com", "s@*.twitter.com", "t@*.twitter.com", "twclid"]},
        { "name": "Yahoo", "params": ["guccounter@*.yahoo.com", "soc_src", "soc_trk"]},
        { "name": "Yandex", "params": ["lr@yandex.*", "redircnt@yandex.*"]},
        { "name": "YouTube.com", "params": ["feature@youtube.com", "kw@youtube.com"]},
        { "name": "Zeit.de", "params": ["wt_mc", "wt_zmc"]}
    ]
}
        """
    }
}