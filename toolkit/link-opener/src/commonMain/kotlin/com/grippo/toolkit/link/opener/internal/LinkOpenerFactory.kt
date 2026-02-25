package com.grippo.toolkit.link.opener.internal

import com.grippo.toolkit.context.NativeContext
import com.grippo.toolkit.link.opener.LinkOpener

internal expect fun NativeContext.getLinkOpener(): LinkOpener
