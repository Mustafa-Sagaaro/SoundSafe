# SoundSafe

SoundSafe misst mithilfe des Mikrofons den Umgebungsgeräuschpegel. Überschreitet der Pegel einen benutzerdefinierten Schwellenwert, wird das Gerät vibrieren und eine Warnbenachrichtigung wird gesendet.

## Was ist anders?

Ich habe die Die Funktionen alle in 3 verschienden Files obwohl ich für jede eine eigene Machen wollte. Ansonsten ist alles genau wie in der Planung.

## Linter

Die Linter-Berichte findest du hier:  
`SoundSafe/app/build/reports`

## Wie man die App testet

1. Starte die App.
2. Erlaube den Zugriff auf das Mikrofon und Benachrichtigungen.
3. Klicke oben rechts auf Settings und stelle ein gewünschtes Limit ein.
4. nun gehe zurück und schaue ob die App die Lautstärke misst und anzeigt und ob sie bei überschreitung des Limits ihnen eine Warnung sendet und vibriert. 
- Teste vorzugsweise auf einem echten Gerät, da der Emulator bei Mikrofoneingaben Probleme haben kann.

## Funktionen

- **Hintergrundüberwachung:** Misst den Lärmpegel kontinuierlich, auch wenn die App geschlossen ist.
- **Warnungen:** Bei Überschreiten des Limits wird eine Vibration ausgelöst und eine Notification angezeigt.
- **Einstellungen:** Der Benutzer kann den Limit (z.B 85 dB) über einen Slider anpassen.

## Schwierigkeiten

Ich hatte Schwierigkeiten mit dem zugriff auf das Mikrofon und das anzeigen, des richtigen dB Wert. Es ging nicht, da ich irgendwie keinen Zugriff auf das Mikrofon gelangen konnte, jedoch hat dies nach einer weile recherchieren und ausprobieren irgendwann geklappt.

## Versionierung

Ich habe während des Projektes ein Git Repo geführt:
https://github.com/Mustafa-Sagaaro/SoundSafe

## Quellen

Bei der Entwicklung der App habe ich folgende Quellen genutzt:

- Für App-Icon:
https://developer.android.com/codelabs/basic-android-kotlin-compose-training-change-app-icon#4

- Microphone Permission:
https://stackoverflow.com/questions/24817849/microphone-permission

- Vibration:
https://stackoverflow.com/questions/45605083/android-vibrate-is-deprecated-how-to-use-vibrationeffect-in-android-api-26

- Chatgpt:
Ich habe Chat gpt als Hilfe genutzt in diesem Projekt, vorallem um Dinge zu verstehen die ich Online nicht finden konnte. Chat gpt hat mir vorallem bei der Fehlerbehebung und Verständnis für Code geholfen. Genutzt habe ich Chatgpt für die Hintergrund Funktion und das senden von Nachrichten.


