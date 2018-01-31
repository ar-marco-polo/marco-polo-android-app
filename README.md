# Marco Polo
## Augmented Reality Project Wintersemester 2017 / 2018
by Manuel Reich & Arne Schlüter

## Intro
Marco Polo is an augmented reality app, that enables people to find themselves over an audio track that they emit from their position. The name is a nod to the hide and seek game in which one person tries to find the other one by following their voice.

The project consists of an Android application and a web server that can be found in [another repository](https://github.com/ar-marco-polo/marco-polo-server). After starting a game a devices' location is continuously tracked over GPS and sent to the servers, which forwards it to other players that have joined the game via websockets.

## Notes on GPS

Our tests have shown that with mid- to high-end Android devices the average accuracy in places with few obstacles (e.g. public parks) is in the range of &lt; 10m to 20m. This is more than sufficient to locate another person. In other scenarios such as streets with buildings of four or five floors on both sides the average accuracy was around 30m. Inside a building the accuracy can easily get higher, which leads to the game quality being very degraded up until the point of it being unplayable.

We evaluated multiple other tracking technologies, amongst others the [find framework](https://github.com/schollz/find) or Bluetooth Beacons for indoor positioning. Unfortunately none matched up with our goal of bringing the game to as many devices in as many situations as possible.

## Attribution

We took the sounds in the game from [freesound.org](https://freesound.org/)
- static noise: https://freesound.org/people/Jace/sounds/35291/
- vintage elecro pop loop : https://freesound.org/people/frankum/sounds/384468/
