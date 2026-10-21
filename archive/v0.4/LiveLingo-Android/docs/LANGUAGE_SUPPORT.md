# LiveLingo language support, October 2026

## What “supported” means

- **Translation**: Google ML Kit Translation on-device supports 59 target languages. Models need a one-time download, usually over Wi-Fi. Offline translation works only once the exact language pair has been installed. Some translations go through English, which can decrease quality.
- **Automatic voice recognition**: Android 14+ provides *optional* speech language detection and switching. Available speech packs, model capacity, language coverage and support vary by Android phone/recognizer. The app requests common source-language switching but never guarantees that every supported translation language can be heard. Android 12–13 automatically identify *recognized text*, not the language of arbitrary raw speech. There is no cloud speech fallback.
- **Definition popup**: small, contextual and disappears on press release; it is not a full dictionary for 59 languages. No arbitrary Wikipedia images or networking.

## Translation language catalog (local ML Kit)

59 codes: `af ar be bg bn ca cs cy da de el en eo es et fa fi fr ga gl gu he hi hr ht hu id is it ja ka kn ko lt lv mk mr ms mt nl no pl pt ro ru sk sl sq sv sw ta te th tl tr uk ur vi zh`.

Notable examples for newcomer language learning:

| Written language | Code | Status | Notes |
|---|---|---|---|
| Arabic العربية | ar | Local translator available | Syrian/Palestinian spoken dialect recognition depends on device; `ar-SY` and `ar-PS` are *speech* tags, not distinct written translation models |
| Persian فارسی | fa | Local translator available | Not automatically accurate for distinct Dari dialects |
| Ukrainian Українська | uk | Local translator available | Voice recognition requires phone's speech pack |
| Turkish Türkçe | tr | Local translator available | Voice recognition requires phone's speech pack |
| Urdu اردو | ur | Local translator available | Voice recognition requires phone's speech pack |
| Russian Русский | ru | Local translator available | Voice recognition requires phone's speech pack |
| Dutch Nederlands | nl | Local translator available | Often used as the target by learners in the Netherlands |
| Swahili Kiswahili | sw | Local translator available | Device voice pack availability differs |
| Kurdish Kurmanji | kmr | **Limited offline phrasebook (5 words/expressions)**; not ML Kit | Selectable preview; general translation & speech ASR require separate offline models |
| Kurdish Sorani | ckb | **Limited offline phrasebook (5 words/expressions)**; not ML Kit | Selectable preview; general translation & speech ASR require separate offline models |
| Pashto پښتو | ps | **Limited offline phrasebook (5 words/expressions)**; not ML Kit | Selectable preview; general translation & speech ASR require separate offline models |
| Somali Soomaali | so | Not supported by ML Kit Translate | Requires separate local model and ASR |
| Tigrinya ትግርኛ | ti | Not supported by ML Kit Translate | Requires separate local model and ASR |
| Amharic አማርኛ | am | Not supported by ML Kit Translate | Requires separate local model and ASR |
| Oromo Afaan Oromoo | om | Not supported by ML Kit Translate | Requires separate local model and ASR |
| Rohingya | rhg | Not supported by ML Kit Translate | Requires separate local model and ASR |
| Dari دری | prs | Dedicated dialect model not supported | Do not mislabel standard Persian as full Dari support |

The picker separates the 59 full ML Kit target languages, 3 **limited vocabulary-only** Kurdish/Pashto preview choices, and future unsupported languages. Preview options support only exact matches of 5 simple expressions recognized as Dutch or English; other speech, longer sentences, and language packs are not supported. Native speakers should verify vocabulary before production release. For general conversation, implement and validate a local speech/translation engine; no cloud fallback or unsafe assumptions.

## Model management

- An initial Wi-Fi preparation loads no more than three useful source→target pairs (phone language, Arabic, English, Dutch in bounded priority order; exclude target and duplicates).
- When another source language is identified, lazily download its **one** source→target model pair if missing. Do not pre-download all 58 others.
- UI should surface that a first-time pair needs Wi-Fi and clarify that only installed models are available without internet.
- Source detection can be wrong on short speech. Don't silently call an unsupported language supported.

## Evidence and references

- Supported translation languages: https://developers.google.com/ml-kit/language/translation/translation-language-support
- ML Kit limitations/English intermediary: https://developers.google.com/ml-kit/language/translation
- Android speech detection/switch API 34+: https://developer.android.com/reference/android/speech/RecognitionListener#onLanguageDetection(android.os.Bundle)

## Kurdish / Pashto future offline engine plan

- **Text translation candidate:** model architecture such as NLLB-200 with `kmr_Latn`, `ckb_Arab`, `pbt_Arab` plus `nld_Latn`/`eng_Latn`; a runtime, tokenizer, quantization, device-memory tests, explicit model download, and independent quality checks are still necessary. The common research checkpoint has CC-BY-NC-4.0 terms; check intended use and provenance before distribution.
- **Offline ASR:** speech-language auto-detection and raw speech recognition are separate from NLLB text translation. Do not use ML Kit speech recognition or silent cloud fallback for unverified Kurdish/Pashto tags. Evaluate quantized local speech models on real dialect-specific audio.
- **Preview vocabulary:** `hello`, `thank you`, `yes`, `no`, `water` only; exact-match input in English or Dutch, with punctuation/whitespace normalization. Other utterances return an explicit error, not a fabricated translation.
- **Acceptance:** offline airplane-mode end-to-end phrase coverage; native-speaker review for all dialects, transliteration and morphology, and device performance tests; never imply general translation availability based on a catalog mapping alone.
- Sources (model mapping): https://huggingface.co/facebook/nllb-200-distilled-600M ; https://huggingface.co/facebook/nllb-200-distilled-600M/blob/main/special_tokens_map.json ; beginner vocabulary: https://yari.krd/learn/kurdish-greetings/ ; https://en.wiktionary.org/wiki/%D8%B3%DA%B5%D8%A7%D9%88 ; https://learnpashtoo.com/pashto-vocabulary/
