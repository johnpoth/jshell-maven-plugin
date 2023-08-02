module com.github.johnpoth.jshell {

   requires jdk.jshell;
   /**not modules yet*/
   requires maven.plugin.api;
   requires maven.plugin.annotations;
   requires maven.artifact;

   exports com.github.johnpoth.jshell;

   uses javax.tools.Tool;
}
