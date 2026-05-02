## javafx-objimporter

And so I thought it would be nice to point to my new library in a Reddit post such that
fellow JavaFX users get aware of just another way to import OBJ files and create JavaFX mesh views from them.

And guess what happened: the same type of "Blockwart" you nowadays find in all "social" networks
stepped in and DELETED my post because parts of the code have been improved using an AI! (My
Reddit post mentioned that in the very first sentence!)

Maybe I am too old for this shit, but obviously the "social" networks have become a place for sociopathic morons. 


### Parsing Wavefront OBJ files and creation of JavaFX mesh views 

![Mesh Viewer App - Alien Animal](images/meshviewer.png)


![Mesh Viewer App - Aya](images/meshviewer-aya.png)

#### Build and publish to local Maven repository

`./gradlew publishToMavenLocal`

#### Sample code for creating a JavaFX mesh view from an OBJ file loaded via an URL:

```
ObjModel objModel = new ObjFileParser(objFileURL, StandardCharsets.UTF_8).parse();
// To build the mesh views for the groups inside the OBJ file:
Map<String, MeshView> meshes = MeshBuilder.build(objModel, MeshBuilder.BuildMode.BY_GROUP);
```

