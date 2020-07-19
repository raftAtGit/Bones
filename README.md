# Bones - Skeletal and Pose Animations for jPCT

![Bones](doc/images/quake3_bones.jpg)

* [Overview](#Overview)
* [Features](#Features)
* [Supported formats](#Supported-formats)
* [Download](#Download)
* [Feedback](#Feedback)
* [Javadocs](#Javadocs)
* [History](#History)
* [Demos](#Demos)

## [Overview](#Overview)

Bones is a small animation library for [jPCT](http://www.jpct.net/). It supports skeletal and pose animations. Bones is pure Java and requires at least Java version 1.5.

Bones is definitely free software with a *"Do whatever with it"* license. **However**, please note, adapted sources and sample model data may still be subject to provider's license terms.

Bones initially started as a partial/modified port of Ardor3D's animation system to jPCT. Then it evolved to match jPCT semantics and load [Ogre3D](https://www.ogre3d.org/) skins and pose animations.

## [Features](#Features)

Bones supports [skeletal](http://www.okino.com/conv/skinning.htm) and [pose](http://www.ogre3d.org/docs/manual/manual_80.html#SEC352) animations. Pose animations can be blended with each other and with a single skin (skeletal) animation. Skeletal animations can not be blended. Scaling and rotation is supported during initial loading. API allows programmatically created animated objects, skeletons and animations. GPU based animations are not supported.

Bones is pure Java and requires at least Java version 1.5. Bones can run on Android with [Android edition of jPCT](http://www.jpct.net/jpct-ae/).

## [Supported formats](#Supported-formats)

**Collada:** Bones can load [Collada](http://www.collada.org/) animation files via Ardor3D's Collada loader. Only mesh data, texture coordinates and skeletal animation data is loaded.

**Ogre3D:** Bones can load [Ogre3D](http://www.ogre3d.org/) animation files via [jME](https://jmonkeyengine.org/)'s OgreXml loader. Only mesh data, texture coordinates and skeletal and pose animation data is loaded.

After initial loading, Bones objects and animation data can be saved in a compact binary form. Saved animations can be later reloaded with loader, with no dependencies to Ardor3D or jME. Either way after loading they are ready in jPCT terms. They have mesh data, textures coordinates and skinning information. It's enough to set their textures and call build() to prepare them to be added into a jPCT world.

Command line scripts are provided to ease importing process.

## [Download](#Download)

Pre-built releases can be found at [releases](releases) page.

## [Feedback](#Feedback)

For discussion, questions and any kind of feedback, visit the [Bones board at jPCT forums](http://www.jpct.net/forum2/index.php/board,10.0.html)

## [Javadocs](#Javadocs)

Javadocs can be found [here](http://aptalkarga.com/bones/api/index.html).

## [History](#History)

Version history can be found [here](HISTORY.md).

## [Demos](#Demos)

Here are screen captures of a few sample applications:

*Facial animation loaded as Ogre3D Pose animation:*

[![Facial animation](https://img.youtube.com/vi/vemKY9kosvI/0.jpg)](https://www.youtube.com/watch?v=vemKY9kosvI)

*Skeletal animation loaded with jME's OgreXml loader:*

[![Skeletal animation](https://img.youtube.com/vi/dGaaxiSwH_Y/0.jpg)](https://www.youtube.com/watch?v=dGaaxiSwH_Y)

*Collada skin procedurally animated (sample adapted from Ardor3d):*

[![Collada skin procedurally animated](https://img.youtube.com/vi/G3MLLsaKKxI/0.jpg)](https://www.youtube.com/watch?v=G3MLLsaKKxI)

*Ninja demo running on Android (captured by EgonOlsen):*

[![Ninja demo running on Android](https://img.youtube.com/vi/gDlNMdXJETk/0.jpg)](https://www.youtube.com/watch?v=gDlNMdXJETk)

*Animation blending:*

[![Animation blending](https://img.youtube.com/vi/nq5q4NmuQVo/0.jpg)](https://www.youtube.com/watch?v=nq5q4NmuQVo)
