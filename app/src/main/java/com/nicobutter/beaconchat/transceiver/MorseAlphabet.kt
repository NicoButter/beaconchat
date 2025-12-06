package com.nicobutter.beaconchat.transceiver

import java.util.Locale

/**
 * Multi-language Morse code alphabet support.
 *
 * Provides Morse code mappings for different writing systems and languages,
 * enabling global communication through light, sound, and vibration.
 *
 * Supported alphabets:
 * - Latin (International Standard - ITU-R M.1677)
 * - Cyrillic (Russian and Slavic languages)
 * - Greek (Ελληνικά)
 * - Hebrew (עברית)
 * - Arabic (العربية)
 * - Japanese (Wabun code - かな)
 * - Korean (Hangul - 한글)
 *
 * @see https://en.wikipedia.org/wiki/Morse_code_for_non-Latin_alphabets
 */
object MorseAlphabet {
    
    /**
     * Gets the appropriate Morse code map based on system locale.
     *
     * @param locale Current system locale (defaults to system default)
     * @return Map of character to Morse code string
     */
    fun getMorseMap(locale: Locale = Locale.getDefault()): Map<Char, String> {
        return when (locale.language) {
            "ru", "uk", "be", "bg", "sr" -> CYRILLIC // Ruso, Ucraniano, Bielorruso, Búlgaro, Serbio
            "el" -> GREEK // Griego
            "he", "iw" -> HEBREW // Hebreo (he=ISO 639-1, iw=legacy)
            "ar" -> ARABIC // Árabe
            "ja" -> JAPANESE // Japonés
            "ko" -> KOREAN // Coreano
            "th" -> THAI // Tailandés
            "fa" -> PERSIAN // Persa/Farsi
            else -> LATIN_INTERNATIONAL // Por defecto: Latino Internacional
        }
    }
    
    /**
     * Gets the name of the Morse alphabet for the current locale.
     */
    fun getAlphabetName(locale: Locale = Locale.getDefault()): String {
        return when (locale.language) {
            "ru", "uk", "be", "bg", "sr" -> "Cyrillic Morse"
            "el" -> "Greek Morse"
            "he", "iw" -> "Hebrew Morse"
            "ar" -> "Arabic Morse"
            "ja" -> "Wabun Code (Japanese)"
            "ko" -> "Korean Morse"
            "th" -> "Thai Morse"
            "fa" -> "Persian Morse"
            else -> "International Morse (Latin)"
        }
    }
    
    /**
     * Gets the flag emoji for the current locale/alphabet.
     * Returns a representative flag for the writing system.
     */
    fun getFlag(locale: Locale = Locale.getDefault()): String {
        return when (locale.language) {
            "ru" -> "🇷🇺" // Ruso - Bandera de Rusia
            "uk" -> "🇺🇦" // Ucraniano - Bandera de Ucrania
            "be" -> "🇧🇾" // Bielorruso - Bandera de Bielorrusia
            "bg" -> "🇧🇬" // Búlgaro - Bandera de Bulgaria
            "sr" -> "🇷🇸" // Serbio - Bandera de Serbia
            "el" -> "🇬🇷" // Griego - Bandera de Grecia
            "he", "iw" -> "🇮🇱" // Hebreo - Bandera de Israel
            "ar" -> when (locale.country) {
                "SA" -> "🇸🇦" // Arabia Saudita
                "EG" -> "🇪🇬" // Egipto
                "AE" -> "🇦🇪" // Emiratos Árabes Unidos
                "MA" -> "🇲🇦" // Marruecos
                "DZ" -> "🇩🇿" // Argelia
                else -> "🇸🇦" // Por defecto: Arabia Saudita
            }
            "ja" -> "🇯🇵" // Japonés - Bandera de Japón
            "ko" -> "🇰🇷" // Coreano - Bandera de Corea del Sur
            "th" -> "🇹🇭" // Tailandés - Bandera de Tailandia
            "fa" -> "🇮🇷" // Persa - Bandera de Irán
            "es" -> when (locale.country) {
                "MX" -> "🇲🇽" // México
                "AR" -> "🇦🇷" // Argentina
                "CO" -> "🇨🇴" // Colombia
                "CL" -> "🇨🇱" // Chile
                "PE" -> "🇵🇪" // Perú
                "VE" -> "🇻🇪" // Venezuela
                else -> "🇪🇸" // España (por defecto)
            }
            "en" -> when (locale.country) {
                "US" -> "🇺🇸" // Estados Unidos
                "GB" -> "🇬🇧" // Reino Unido
                "CA" -> "🇨🇦" // Canadá
                "AU" -> "🇦🇺" // Australia
                "NZ" -> "🇳🇿" // Nueva Zelanda
                else -> "🇬🇧" // Reino Unido (por defecto)
            }
            "fr" -> when (locale.country) {
                "CA" -> "🇨🇦" // Canadá
                "BE" -> "🇧🇪" // Bélgica
                "CH" -> "🇨🇭" // Suiza
                else -> "🇫🇷" // Francia (por defecto)
            }
            "de" -> when (locale.country) {
                "AT" -> "🇦🇹" // Austria
                "CH" -> "🇨🇭" // Suiza
                else -> "🇩🇪" // Alemania (por defecto)
            }
            "it" -> "🇮🇹" // Italiano
            "pt" -> when (locale.country) {
                "BR" -> "🇧🇷" // Brasil
                else -> "🇵🇹" // Portugal (por defecto)
            }
            "zh" -> when (locale.country) {
                "CN" -> "🇨🇳" // China
                "TW" -> "🇹🇼" // Taiwán
                "HK" -> "🇭🇰" // Hong Kong
                else -> "🇨🇳" // China (por defecto)
            }
            "pl" -> "🇵🇱" // Polaco
            "nl" -> "🇳🇱" // Holandés
            "sv" -> "🇸🇪" // Sueco
            "no" -> "🇳🇴" // Noruego
            "da" -> "🇩🇰" // Danés
            "fi" -> "🇫🇮" // Finlandés
            "tr" -> "🇹🇷" // Turco
            "vi" -> "🇻🇳" // Vietnamita
            "id" -> "🇮🇩" // Indonesio
            "hi" -> "🇮🇳" // Hindi
            else -> "🌍" // Internacional (icono de mundo)
        }
    }
    
    // ==================== LATIN INTERNATIONAL ====================
    // ITU-R M.1677 - International Morse Code Standard
    private val LATIN_INTERNATIONAL = mapOf(
        // Letters
        'A' to ".-", 'B' to "-...", 'C' to "-.-.", 'D' to "-..",
        'E' to ".", 'F' to "..-.", 'G' to "--.", 'H' to "....",
        'I' to "..", 'J' to ".---", 'K' to "-.-", 'L' to ".-..",
        'M' to "--", 'N' to "-.", 'O' to "---", 'P' to ".--.",
        'Q' to "--.-", 'R' to ".-.", 'S' to "...", 'T' to "-",
        'U' to "..-", 'V' to "...-", 'W' to ".--", 'X' to "-..-",
        'Y' to "-.--", 'Z' to "--..",
        
        // Numbers
        '0' to "-----", '1' to ".----", '2' to "..---", '3' to "...--",
        '4' to "....-", '5' to ".....", '6' to "-....", '7' to "--...",
        '8' to "---..", '9' to "----.",
        
        // Special characters with diacritics
        'Á' to ".--.-", 'À' to ".--.-", 'Ä' to ".-.-", 'Å' to ".--.-",
        'É' to "..-..", 'È' to ".-..-", 'Ñ' to "--.--",
        'Ö' to "---.", 'Ü' to "..--", 'Ç' to "-.-..",
        
        // Punctuation
        '.' to ".-.-.-", ',' to "--..--", '?' to "..--..",
        '\'' to ".----.", '!' to "-.-.--", '/' to "-..-.",
        '(' to "-.--.", ')' to "-.--.-", '&' to ".-...",
        ':' to "---...", ';' to "-.-.-.", '=' to "-...-",
        '+' to ".-.-.", '-' to "-....-", '_' to "..--.-",
        '"' to ".-..-.", '$' to "...-..-", '@' to ".--.-."
    )
    
    // ==================== CYRILLIC (Russian) ====================
    // 33 letters of Russian alphabet + Slavic extensions
    private val CYRILLIC = mapOf(
        // Russian Cyrillic (33 letters)
        'А' to ".-", 'Б' to "-...", 'В' to ".--", 'Г' to "--.",
        'Д' to "-..", 'Е' to ".", 'Ж' to "...-", 'З' to "--..",
        'И' to "..", 'Й' to ".---", 'К' to "-.-", 'Л' to ".-..",
        'М' to "--", 'Н' to "-.", 'О' to "---", 'П' to ".--.",
        'Р' to ".-.", 'С' to "...", 'Т' to "-", 'У' to "..-",
        'Ф' to "..-.", 'Х' to "....", 'Ц' to "-.-.", 'Ч' to "---.",
        'Ш' to "----", 'Щ' to "--.-", 'Ъ' to "--.--", 'Ы' to "-.--",
        'Ь' to "-..-", 'Э' to "..-..", 'Ю' to "..--", 'Я' to ".-.-",
        
        // Ukrainian extensions
        'Є' to "..-..", 'І' to "..", 'Ї' to ".---.", 'Ґ' to "--.",
        
        // Numbers (same as Latin)
        '0' to "-----", '1' to ".----", '2' to "..---", '3' to "...--",
        '4' to "....-", '5' to ".....", '6' to "-....", '7' to "--...",
        '8' to "---..", '9' to "----."
    )
    
    // ==================== GREEK ====================
    // Greek alphabet (24 letters + diacritics)
    private val GREEK = mapOf(
        'Α' to ".-", 'Β' to "-...", 'Γ' to "--.", 'Δ' to "-..",
        'Ε' to ".", 'Ζ' to "--..", 'Η' to "....", 'Θ' to "-.-.",
        'Ι' to "..", 'Κ' to "-.-", 'Λ' to ".-..", 'Μ' to "--",
        'Ν' to "-.", 'Ξ' to "-..-", 'Ο' to "---", 'Π' to ".--.",
        'Ρ' to ".-.", 'Σ' to "...", 'Τ' to "-", 'Υ' to "-.--",
        'Φ' to "..-.", 'Χ' to "----", 'Ψ' to "--.-", 'Ω' to ".--",
        
        // Numbers
        '0' to "-----", '1' to ".----", '2' to "..---", '3' to "...--",
        '4' to "....-", '5' to ".....", '6' to "-....", '7' to "--...",
        '8' to "---..", '9' to "----."
    )
    
    // ==================== HEBREW ====================
    // Hebrew alphabet (22 letters)
    private val HEBREW = mapOf(
        'א' to ".-", 'ב' to "-...", 'ג' to "--.", 'ד' to "-..",
        'ה' to "---", 'ו' to ".", 'ז' to "--..", 'ח' to "....",
        'ט' to "..-", 'י' to "..", 'כ' to "-.-", 'ך' to "-.-",
        'ל' to ".-..", 'מ' to "--", 'ם' to "--", 'נ' to "-.",
        'ן' to "-.", 'ס' to "-.-.", 'ע' to ".---", 'פ' to ".--.",
        'ף' to ".--.", 'צ' to ".--", 'ץ' to ".--", 'ק' to "--.-",
        'ר' to ".-.", 'ש' to "...", 'ת' to "-",
        
        // Numbers
        '0' to "-----", '1' to ".----", '2' to "..---", '3' to "...--",
        '4' to "....-", '5' to ".....", '6' to "-....", '7' to "--...",
        '8' to "---..", '9' to "----."
    )
    
    // ==================== ARABIC ====================
    // Arabic alphabet (28 letters)
    private val ARABIC = mapOf(
        'ا' to ".-", 'ب' to "-...", 'ت' to "-", 'ث' to "-.-.",
        'ج' to ".---", 'ح' to "....", 'خ' to "---", 'د' to "-..",
        'ذ' to "--..", 'ر' to ".-.", 'ز' to "---.", 'س' to "...",
        'ش' to "----", 'ص' to "-..-", 'ض' to "...-", 'ط' to "..-",
        'ظ' to "-.--", 'ع' to ".-.-", 'غ' to "--.", 'ف' to "..-.",
        'ق' to "--.-", 'ك' to "-.-", 'ل' to ".-..", 'م' to "--",
        'ن' to "-.", 'ه' to "..-..", 'و' to ".--", 'ي' to "..",
        
        // Numbers (Eastern Arabic)
        '٠' to "-----", '١' to ".----", '٢' to "..---", '٣' to "...--",
        '٤' to "....-", '٥' to ".....", '٦' to "-....", '٧' to "--...",
        '٨' to "---..", '٩' to "----.",
        
        // Western Arabic numbers
        '0' to "-----", '1' to ".----", '2' to "..---", '3' to "...--",
        '4' to "....-", '5' to ".....", '6' to "-....", '7' to "--...",
        '8' to "---..", '9' to "----."
    )
    
    // ==================== JAPANESE (Wabun Code) ====================
    // Japanese Morse uses Kana (Hiragana/Katakana)
    private val JAPANESE = mapOf(
        // Hiragana
        'あ' to "--.--", 'い' to ".-", 'う' to "..-", 'え' to "-.---",
        'お' to ".-...", 'か' to ".-..", 'き' to "-.-..", 'く' to "...-",
        'け' to "-.--", 'こ' to "----", 'さ' to "-.-.-", 'し' to "--.-.",
        'す' to "---.-", 'せ' to ".---.", 'そ' to "---.", 'た' to "-.",
        'ち' to "..-.", 'つ' to ".--.", 'て' to ".-.--", 'と' to "..-..",
        'な' to ".-.", 'に' to "-.-.", 'ぬ' to "....", 'ね' to "--.-",
        'の' to "..--", 'は' to "-...", 'ひ' to "--..-", 'ふ' to "--..",
        'へ' to ".", 'ほ' to "-..", 'ま' to "-..-", 'み' to "..-.-",
        'む' to "-", 'め' to "-...-", 'も' to "-..-.", 'や' to ".--",
        'ゆ' to "-..--", 'よ' to "--", 'ら' to "...", 'り' to "--.",
        'る' to "-.--.", 'れ' to "---", 'ろ' to ".-.-", 'わ' to "-.-",
        'ゐ' to ".-..-", 'ゑ' to ".--..", 'を' to ".---", 'ん' to ".-.-.",
        
        // Numbers
        '0' to "-----", '1' to ".----", '2' to "..---", '3' to "...--",
        '4' to "....-", '5' to ".....", '6' to "-....", '7' to "--...",
        '8' to "---..", '9' to "----."
    )
    
    // ==================== KOREAN (Hangul) ====================
    // Korean Morse uses Hangul consonants and vowels
    private val KOREAN = mapOf(
        // Consonants
        'ㄱ' to ".-.", 'ㄴ' to "..-.", 'ㄷ' to "-...", 'ㄹ' to "...-",
        'ㅁ' to "--", 'ㅂ' to ".--", 'ㅅ' to "--.", 'ㅇ' to "-..",
        'ㅈ' to ".--.", 'ㅊ' to "-.-.", 'ㅋ' to "-..-", 'ㅌ' to "--..",
        'ㅍ' to "---", 'ㅎ' to ".---",
        
        // Vowels
        'ㅏ' to ".", 'ㅑ' to "..", 'ㅓ' to "-", 'ㅕ' to "...",
        'ㅗ' to ".-", 'ㅛ' to "-..", 'ㅜ' to "....", 'ㅠ' to ".-.",
        'ㅡ' to "-..-", 'ㅣ' to "..-",
        
        // Numbers
        '0' to "-----", '1' to ".----", '2' to "..---", '3' to "...--",
        '4' to "....-", '5' to ".....", '6' to "-....", '7' to "--...",
        '8' to "---..", '9' to "----."
    )
    
    // ==================== THAI ====================
    // Thai alphabet (44 consonants + vowels)
    private val THAI = mapOf(
        // Thai consonants (selection - full alphabet is extensive)
        'ก' to "--..", 'ข' to "-..-", 'ค' to "-.-", 'ง' to "--.--",
        'จ' to "-.-..", 'ฉ' to "----", 'ช' to "-..-.", 'ซ' to "--.-",
        'ญ' to ".---", 'ฎ' to "-..", 'ฏ' to "-", 'ฐ' to "-.--.",
        'ฑ' to ".-.-", 'ฒ' to "--.-.", 'ณ' to "-.", 'ด' to "-..",
        'ต' to "-", 'ถ' to "-.--.", 'ท' to "-..--", 'ธ' to "..-..",
        'น' to "-.", 'บ' to "-...", 'ป' to ".--.", 'ผ' to "--.-",
        'ฝ' to "-..-.", 'พ' to ".--.", 'ฟ' to "..-.", 'ภ' to ".--.",
        'ม' to "--", 'ย' to "-.--", 'ร' to ".-.", 'ล' to ".-..",
        'ว' to ".--", 'ศ' to "...", 'ษ' to "---.", 'ส' to "...",
        'ห' to "....", 'ฬ' to ".-..", 'อ' to "-...-", 'ฮ' to "--..-",
        
        // Numbers
        '0' to "-----", '1' to ".----", '2' to "..---", '3' to "...--",
        '4' to "....-", '5' to ".....", '6' to "-....", '7' to "--...",
        '8' to "---..", '9' to "----."
    )
    
    // ==================== PERSIAN (Farsi) ====================
    // Persian alphabet (32 letters)
    private val PERSIAN = mapOf(
        'ا' to ".-", 'ب' to "-...", 'پ' to ".--.", 'ت' to "-",
        'ث' to "-.-.", 'ج' to ".---", 'چ' to "---.", 'ح' to "....",
        'خ' to "-..-", 'د' to "-..", 'ذ' to "...-", 'ر' to ".-.",
        'ز' to "--..", 'ژ' to "--.", 'س' to "...", 'ش' to "----",
        'ص' to "..-", 'ض' to "..-..", 'ط' to "..-", 'ظ' to "-.--",
        'ع' to "---", 'غ' to "--.-", 'ف' to "..-.", 'ق' to ".-.-",
        'ک' to "-.-", 'گ' to "--.", 'ل' to ".-..", 'م' to "--",
        'ن' to "-.", 'و' to ".--", 'ه' to ".", 'ی' to "..",
        
        // Numbers
        '۰' to "-----", '۱' to ".----", '۲' to "..---", '۳' to "...--",
        '۴' to "....-", '۵' to ".....", '۶' to "-....", '۷' to "--...",
        '۸' to "---..", '۹' to "----.",
        
        // Western numbers
        '0' to "-----", '1' to ".----", '2' to "..---", '3' to "...--",
        '4' to "....-", '5' to ".....", '6' to "-....", '7' to "--...",
        '8' to "---..", '9' to "----."
    )
}
