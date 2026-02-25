package com.grippo.toolkit.link.opener.internal

import com.grippo.toolkit.context.NativeContext
import com.grippo.toolkit.link.opener.LinkOpener

internal actual fun NativeContext.getLinkOpener(): LinkOpener {
    return AppleLinkOpener()
}
