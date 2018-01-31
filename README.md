# Marco Polo
## Augmented Reality Project Wintersemester 2017 / 2018
by Manuel Reich & Arne Schlüter

## Intro
Marco Polo is an augmented reality app, that enables people to find themselves over an audio track that they emit from their position. The name is a nod to the hide and seek game in which one person tries to find the other one by following their voice.

The project consists of an Android application and a web server that can be found in [another repository](https://github.com/ar-marco-polo/marco-polo-server). After starting a game a devices' location is continuously tracked over GPS and sent to the servers, which forwards it to other players that have joined the game via websockets.
