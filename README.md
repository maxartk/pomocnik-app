# ⚡ Pomocnik — Mobilní asistent pro evidenci údržby

> Kotlin + Jetpack Compose Android aplikace pro rychlé zapisování a překlad technických zpráv z ukrajinštiny do češtiny.

## 🎯 Funkce

- 🎤 **Hlasové zadávání** — diktujte popis práce v ukrajinštině
- 🤖 **AI překlad** — automatický překlad UA→CZ pomocí Gemini 2.0 Flash (OpenRouter)
- 📸 **Připojení fotky** — fotografie z galerie nebo z fotoaparátu
- 📝 **Technický report** — AI generuje strukturovanou technickou zprávu
- 💾 **Lokální uložení** — vše uloženo lokálně (Room DB)
- 📋 **Historie** — vyhledávání a správa všech záznamů
- 📤 **Sdílení** — export záznamu přes systémové sdílení (Telegram, Email, atd.)
- 👤 **Profil** — jméno, email, výchozí hodnoty

## 🏗️ Technologie

| Komponenta | Technologie |
|---|---|
| UI | Jetpack Compose + Material 3 |
| Database | Room (SQLite) |
| API | Retrofit + OkHttp |
| AI | OpenRouter API (Gemini 2.0 Flash) |
| Storage | DataStore Preferences |
| Architektura | MVVM + Repository |
| Async | Kotlin Coroutines + Flow |

## 📱 Struktura aplikace

```
app/src/main/java/cz/kovmak/pomocnik/
├── PomocnikApp.kt              # Application class
├── data/
│   ├── database/
│   │   ├── AppDatabase.kt      # Room databáze
│   │   ├── WorkEntry.kt        # Entita záznamu
│   │   └── WorkEntryDao.kt     # DAO rozhraní
│   ├── network/
│   │   └── OpenRouterApi.kt    # API klient pro OpenRouter
│   ├── repository/
│   │   └── WorkRepository.kt   # Repository s AI logikou
│   └── settings/
│       └── SettingsRepository.kt # DataStore pro nastavení
├── ui/
│   ├── MainActivity.kt          # Hlavní aktivita s navigací
│   ├── screens/
│   │   ├── HomeScreen.kt        # Nový záznam
│   │   ├── HistoryScreen.kt     # Historie záznamů
│   │   └── SettingsScreen.kt    # Nastavení
│   └── theme/
│       └── Theme.kt             # Material 3 téma
└── viewmodel/
    ├── WorkViewModel.kt         # ViewModel pro zadání
    ├── HistoryViewModel.kt      # ViewModel pro historii
    └── SettingsViewModel.kt     # ViewModel pro nastavení
```

## 🚀 Setup

1. **Získejte API klíč** na [openrouter.ai](https://openrouter.ai)
2. **Otevřete aplikaci** → Nastavení → zadejte API klíč
3. **Vyplňte profil** — jméno a email pro záznamy

## 📋 Workflow

```
Diktujte/Napište (UA) → AI Překlad (CZ) → Ulož → Sdílej
```

### Vytvoření záznamu:
1. Zadejte číslo zakázky
2. Vyberte typ práce (Elektrická/Mechanická)
3. Nastavte čas začátku a konce
4. **Napište nebo nadiktujte** popis práce (ukrajinsky)
5. Stiskněte **"Přeložit do češtiny"**
6. Přidejte materiály a volitelně foto
7. **Uložte** — záznam je uložen lokálně
8. **Sdílejte** — export přes systémové sdílení

## 🔧 Build

```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease
```

## 📦 Export

Záznamy lze sdílet ve formátu textu přes:
- Telegram
- Email
- WhatsApp
- Kopírování do schránky
- Jakoukoli aplikaci podporující ACTION_SEND

## 📄 Licence

MIT

---

*Autor: Maxim Kovmak*
