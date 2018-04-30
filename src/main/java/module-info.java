module com.github.johnpoth.jshell {

   requires jdk.jshell;
   /**not modules yet*/
   requires maven.plugin.api;
   requires maven.plugin.annotations;

   exports com.github.johnpoth.jshell;

   uses javax.tools.Tool;
}