package com.cq61tech.chilledandroid

/**
 * Zaman cizelgesi. Tum degerler orijinal chilledwindows.exe icinden (kare numaralari,
 * 30 fps -> kare = ms / 33.33333) ve orijinal videodan olculerek alindi.
 */
object Timeline {
    const val FRAME_MS = 33.33333

    // Faz 1: tam ekran goruntu, A kareleri geldikce yatay ayna cevirme (flip)
    // Kare 286'dan itibaren dondurulmus + yakinlastirilmis (siyah koseler olusur)
    const val ROT_START = 286      // 9.53 sn
    const val ROT_SWITCH = 355     // 11.83 sn: aci yonu degisir
    const val ROT_DEG = 46f
    const val BLACK_CORNER_FRACTION = 0.0231f   // videoda siyah kose orani

    // Faz 2: iki sutun (twoGrid), sol B, sag C kareleriyle ayna cevirir
    const val PHASE1_END = 438     // 14.60 sn
    const val PHASE2_END = 585     // 19.50 sn

    // Faz 3: alt+tab ekrani (video iceriginde de 20.73 sn'de baslar)
    const val ALTTAB = 622         // 20.73 sn
    const val ALT_FADE_MS = 300

    // Faz 4: video (BSOD / hata pencereleri) bu andan itibaren gorunur
    const val VIDEO_START_MS = 23000

    val A = intArrayOf(
        126, 129, 133, 136, 140, 143, 147, 150, 155, 158, 162, 165,
        169, 172, 176, 179, 184, 187, 191, 194, 198, 201, 205, 208,
        213, 216, 220, 223, 227, 230, 234, 237, 242, 245, 249, 252,
        256, 259, 263, 266, 271, 274, 278, 281, 285, 286, 288, 292,
        295, 297, 300, 303, 307, 310, 314, 317, 321, 324, 329, 332,
        336, 339, 343, 346, 350, 353, 355, 358, 361, 365, 368, 372,
        375, 379, 382, 387, 390, 394, 397, 401, 404, 408, 411, 416,
        419, 423, 426, 430, 433, 437,
    )

    val B = intArrayOf(
        454, 476, 497, 512, 531, 548, 569, 585,
    )

    val C = intArrayOf(
        440, 443, 449, 454, 456, 460, 468, 476, 481, 486, 497, 501,
        504, 506, 512, 514, 518, 522, 527, 531, 533, 536, 540, 548,
        552, 557, 561, 563, 569, 572, 576, 580, 582, 585,
    )

    fun countLE(arr: IntArray, frame: Int): Int {
        var n = 0
        for (v in arr) if (v <= frame) n++ else break
        return n
    }
}
