# FileBackedCollections
Java extensions to the Collections framework that allow using Collections larger than the computer memory.  
The collections store data in a file, but act like the normal collections.

They are useful when storing a large number of objects larger than 8 byte floats/integers becauese it uses a random access
file to store the data.  The index for the objects is held in memory for high performance so this framework does not help
if the issue is about a large number of small objects.  

The classes are designed as direct replacements for ArrayList,  Set, and HashMap collections 
for example:<br>   ArrayList\<String\>  myList = new FileBackedArrayList\<String\>();
