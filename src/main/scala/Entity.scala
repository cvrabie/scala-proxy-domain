/**
 * User: cvrabie1
 * Date: 14/11/2012
 */

/**
* Base for all domain objects
*/
trait Entity {}

/**
 * Trait for entities that have a primary key
 * @tparam ID Type of the primary key
 */
trait Id[ID]{
  val id:ID
}

/**
 * Base for all entity proxies
 * @tparam E The type of entity being proxied
 */
trait EntityProxy[E <: Entity]{
  protected def fetch():Option[E]
  lazy val entityMaybe:Option[E] = fetch()
}

/**
 * Helper object that contains the implicit conversions from entity proxy to entity
 */
object EntityProxy{
  implicit def EntityProxyToEntityOption[E <: Entity](ep:EntityProxy[E]):Option[E] = ep.entityMaybe
  implicit def GuaranteedEntityProxyToEntity[E <: Entity](ep:EntityProxy[E] with GuaranteedEntityProxy[E]):E = ep.entity
}

/**
 * Trait that can be mixed into an EntityProxy that can guarantee that it can fetch it's entity. The advantage is
 * that such a proxy can be implicitly converted to E, instead of Option[E], then used like the proxy isn't there.<br />
 * By default this works by just throwing an exception in case the fetch fails, but the behavior can be overridden
 * to provide a default value for example. To do this, override #fetchErrorResolution() to return the default value.
 * @tparam E
 */
trait GuaranteedEntityProxy[E <: Entity] extends EntityProxy[E]{
    lazy val entity:E = entityMaybe.getOrElse(fetchErrorResolution())
    protected def fetchErrorResolution():E = throw new RuntimeException("Could not fetch the entity for proxy %s".format(this))
}

trait EntityCollectionProxy[E <: Entity]{
  protected  def fetch():Option[Seq[E]]
  protected lazy val items:Seq[E] = fetch().getOrElse(fetchErrorResolution())
  protected def fetchErrorResolution():Seq[E] = throw new RuntimeException("Could not fetch items for collection proxy %s".format(this))
}

/**
 * Entity proxy that knows how to retrieve its entity based on the primary key
 * @param id The id of the entity being proxied
 * @tparam E The type of entity being proxied
 * @tparam ID The type of the primary key for the entity being proxied
 */
abstract class IdEntityProxy[E <: Entity with Id[ID],ID](id:ID) extends EntityProxy[E]

/**
 * Trait for DAO objects
 * @tparam E The type of the entity this DAO knows to retrieve
 * @tparam ID The type of the primary key of E
 */
trait EntityDao[E <: Entity with Id[ID], ID]{
  def findById(id:ID):Option[E]
}

trait EntityCollectionDao[E <: Entity, OwnerID]{
  def listByOwnerId(ownerId:OwnerID):Option[Seq[E]]
}

/**
 * Entity proxy that relies on a dao to retrieve the proxied entity based on its primary key
 * @param id The id of the entity being proxied
 * @param dao The dao object that knows to retrieve the entity
 * @tparam E The type of entity being proxied
 * @tparam ID The type of the primary key for the entity being proxied
 */
abstract class DaoEntityProxy[E <: Entity with Id[ID], ID](id:ID, dao:EntityDao[E,ID]) extends IdEntityProxy(id){
  protected def fetch():Option[E] = dao.findById(id)
}

/**
 * Entity proxy that relies on a dao to retrieve a collection of entities based on the id of the "owner"
 * @param ownerId
 * @param dao
 * @tparam E
 * @tparam OwnerID
 */
abstract class DaoEntityCollectionProxy[E <: Entity, OwnerID](ownerId:OwnerID, dao:EntityCollectionDao[E,OwnerID]) extends EntityCollectionProxy[E]{
  protected def fetch() = dao.listByOwnerId(ownerId)
}

/**
 * Entity proxy that already has the entity. This is useful in many situations, like when you join multiple tables so
 * you can instantiate all (or some) of the objects.
 * @param e  The actual entity to which
 * @tparam E The type of entity being proxied
 */
abstract class PrefetchedEntityProxy[E <: Entity](e:E) extends EntityProxy[E]{
  protected def fetch() = Some(e)
}

abstract class PrefetchedEntityCollectionProxy[E <: Entity](entities:Seq[E]) extends EntityCollectionProxy[E]{
  protected def fetch() = Some(entities)
}

/**
 * Trait to be implemented by companion object of the classes that want to be proxied by other objects.
 * This is not necessary; you can define all the
 * @tparam E
 * @tparam ID
 */
trait Proxyable[E <: Entity with Id[ID],ID]{
  val dao:EntityDao[E,ID]

  type Proxy = EntityProxy[E]
  type GuaranteedProxy = GuaranteedEntityProxy[E]

  case class DaoProxy(id:ID) extends DaoEntityProxy[E,ID](id,dao) with Proxy
  case class GuaranteedDaoProxy(id:ID) extends DaoEntityProxy[E,ID](id,dao) with GuaranteedProxy
  case class PrefetchedProxy(e:E) extends PrefetchedEntityProxy[E](e) with GuaranteedProxy

  type CollectionProxy = EntityCollectionProxy[E]
  case class DaoCollectionProxy[OwnerID](ownerId:OwnerID, dao:EntityCollectionDao[E,OwnerID])
    extends DaoEntityCollectionProxy[E,OwnerID](ownerId, dao) with CollectionProxy
  case class PrefetchedCollectionProxy(entities:Seq[E])
    extends PrefetchedEntityCollectionProxy(entities) with CollectionProxy

}

/*************************/

case class A(val id:Long, name:String) extends Entity with Id[Long]

object A extends Proxyable[A,Long]{
  //mock dao
  val dao = new EntityDao[A,Long] {
    def findById(id: Long) = Some(A(13,"Sarah"))
  }
}

case class B(val id:Long, val a:A.GuaranteedProxy) extends Entity with Id[Long]

object B{
  def apply(id:Long, a:A) = new B(id, A.PrefetchedProxy(a))
  def apply(id:Long, aId:Long) = new B(id, A.GuaranteedDaoProxy(aId))
}

case class C(val id:Long, val aa:A.CollectionProxy) extends Entity with Id[Long]

object C{
  def apply(id:Long, aa:Seq[A]) = new C()
}

/*************************/

object Test{
  //instantiating B object when we already have A
  val b1 = B(2, A(5, "Jimmy"))
  //instantiating B object when we only know A's id
  val b2 = B(2, 5)

  //use both objects in the same way
  import EntityProxy.GuaranteedEntityProxyToEntity
  println( b1.a.name )
  println( b2.a.name )
}