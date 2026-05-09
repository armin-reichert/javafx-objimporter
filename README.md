## javafx-objimporter

### Parsing Wavefront OBJ files and creation of JavaFX mesh views 

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

### Using the library from another Gradle project

```
dependencies {
    implementation("de.amr66.objimporter:objparser:0.0.1")
    implementation("de.amr66.objimporter:meshbuilder:0.0.1")
}

```
Your application's module-info.java must have the following entries:

```
requires de.amr.objparser;
requires de.amr.meshbuilder;

exports whatever.your.app.module.is.named;
```

(Download from Maven Central not yet available)

### Adding your own sample models

The application creates a folder `$HOME/.meshviewerfx/samples` under your home directory. 
To add your own samples, you have to create a subdirectory for each sample in this folder and register the sample in a TOC file named `toc.json`
which has to be placed in the folder above. 

The format of this file is like this:

```
[
  {
    "title": "Teapot",
    "author": "Martin Newell, Jim Blinn",
    "fileName": "teapot.obj",
    "path": "newell_teaset/",
    "downloadURL": "https://www.cs.utah.edu/~natevm/newell_teaset/newell_teaset.zip",
    "homepage": "https://graphics.cs.utah.edu/teapot/",
    "licenseType": "CC 4.0",
    "licenseURL": "https://creativecommons.org/licenses/by/4.0/legalcode",
    "initSettings": {
      "zoom": -10,
      "rotateX": 0,
      "rotateY": 0,
      "rotateZ": 0,
      "initialMeshSelection": "ALL_GROUPS",
      "wireframe": false,
      "autorotate": true
    }
  }
    
  ...more entries, separated by commata

]
```
Here, the `path` attribute must store the relative path to your model from the `samples` folder and should be terminated with a `/`.
In the `initSettings` object, you can specify the initial preview settings which may vary between samples such that the sample is e.g. zoomed
appropriately when displayed for the first time.

For the TOC file shown above, there must exist a folder `$HOME/.meshviewerfx/samples/newwell_teaset` containing on OBJ file named `teapot.obj`.

For each registered sample, the menu "User Samples" will create an entry where you can select the OBJ file and preview it.


