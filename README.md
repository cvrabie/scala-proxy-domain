scala-proxy-domain is not a framework. It's not even a library. It's an idea about how to create and organise your domain objects to solve some of the problems and peeves commonly encountered when using various libraries and frameworks to handle database operations. 

It does not claim to solve all your problems or work in all corner cases and will definitely add some overhead to the process of designing your domain model, but I think that this is how it should be. You should spend more time designing your basic entities to save yourself a lot of problems later on, because let's face it, you're going to spend more time using those entities than whatever time you invest in designing them. 

So here's a summary of how it works and what problems it helps you to solve and how:

## Solutions to problems/peeves

### 1. Have control over the hierarchy of your classes

TODO

### 2. Use immutable objects if you want to

TODO

### 3. Fetch multiple, different entities at once with joins

TODO

### 4. Store a referenced, already fetched, entity directly in the referencing object

TODO

## How it works

### Your entities

You will start by mixing the **Entity** trait into your domain classes to act as a marker. Notice that this does not imposes any restriction on the hierarchy of your entities like other frameworks that require you to extend specific classes. 

If your entity has a primary key, your should also mix the **Id[T]** trait into your class.

	case class Human(id:Long, name:String) extends Entity with Id[Long]
	
If this class will be referenced by another class you can also create a companion object for your class that mixes in the **Proxyable** trait. This is not necessary but it helps define some types that you will need, like the custom **EntityProxy**. You can define these types yourself and it's not necessary at all if your entity only references other entities but is not referenced back.

	object Human extends Proxyable[User,Long]{
		val dao = null
	}
	
The Proxyable trait will ask your companion object to specify an **EntityDao** which is basically an object that knows how to retrieve an object of your class based on its primary key. However, if your handle this through other mechanisms you can just set it to null.
   

### 1-1 Relations
	
Your entity has properties like a normal class. However, when your want to reference another entity in a 1-1 relation your should use an **EntityProxy**

	case class Car(val id:Long, val driver:Human.Proxy) extends Entity with Id[Long]
	
This is equivalent to 

	case class Car(val id:Long, val driver:Option[Human])
	
Notice that the relation is to an Option of the other class. This is mainly to allow null values and to allow silent failure in case the referenced entity is not found in the database or the fetch fails. However, if you can guarantee that there always will be an referenced entity, like in case there's a default or you want a failure in fetch to result in an exception, you can use a **GuaranteedProxy**.

	case class Car(val id:Long, val driver:Human.GuaranteedProxy) extends Entity with Id[Long]
	
This is equivalent to 
	
	case class Car(val id:Long, val driver:Human)
	
Using a GuaranteedProxy allows an implicit conversion to the actual entity with *EntityProxy.GuaranteedEntityProxyToEntity*

It's also useful to define apply methods in the companion object that allows instantiation with actual entities, or with the id of the referenced entity, so the constructor usage is more natural.

	object Car{
		def apply(id:Long, driver:Human) = new Car(2, Human.PrefetchedProxy(driver))
		def apply(id:Long, driverId:Long) = new Car(2, Human.GuaranteedDaoProxy(driverId))
	}

### Using the entities

The best way to use the entities is to use the implicit conversion from **EntityProxy** like **EntityProxy.GuaranteedEntityProxyToEntity**. This allows using the referenced entities like there is not proxy at all:

	val c1 = Car(2, Human(5, "Jimmy"))
	val c2 = Car(2, 6)
	import EntityProxy.GuaranteedEntityProxyToEntity
	println( c1.driver.name )
	println( c2.driver.name )
	
![Class inner structure](http://yuml.me/8849be87 "")