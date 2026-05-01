# javafx-objimporter

Parsing Wavefront OBJ files and creation of JavaFX mesh views 

![Mesh Viewer App - Alien Animal](images/meshviewer.png)


![Mesh Viewer App - Aya](images/meshviewer-aya.png)

### Build and publish to local Maven repository

`./gradlew publishToMavenLocal`

### Sample code for creating a JavaFX mesh view from an OBJ file loaded via an URL:

```
ObjModel objModel = new ObjFileParser(objFileURL, StandardCharsets.UTF_8).parse();
// To build the mesh views for the groups inside the OBJ file:
Map<String, MeshView> meshes = MeshBuilder.build(objModel, MeshBuilder.BuildMode.BY_GROUP);
```

