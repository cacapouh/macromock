package macromock
import annotation.experimental
import scala.reflect.ClassTag

@experimental
object StubFactory {

  import scala.quoted.*

  private val mockClassName = "StubTargetImpl"

  inline def stub[T]: T = ${ createStubInstance[T] }

  private def createStubInstance[T](using Type[T], Quotes): Expr[T] = {
    import quotes.reflect.*
    if(!TypeRepr.of[T].typeSymbol.toString.startsWith("trait ")) { // TODO: トレイトかどうかの判別を文字列でしないようにする
      // TODO: 抽象クラスや具象クラスもサポートするようにする
      throw new NotImplementedError("Stubing values of type T other than traits is not supported.")
    }

    // トレイトで定義しているが実装は無いようなメソッド
    val notImplementedMethods = TypeRepr.of[T].typeSymbol.methodMembers
      .filter { member =>
        member.flags.is(Flags.Deferred)
      }

    def newMethodType(methodDeclaration: Symbol) = {
      // TODO: def hoge(x: Int)(y: Int)みたいなカリー化されたメソッドにも対応
      val paramNames = methodDeclaration.paramSymss.flatten.map(_.name)
      val paramTypeRepr = methodDeclaration.signature.paramSigs.collect {
        case className: String => Symbol.classSymbol(className).typeRef
      }
      val resultTypeRepr = if(methodDeclaration.signature.resultSig.isEmpty){
        // TODO: def hoge: Int のような引数なしのメソッドもサポートする
        // FIXME: 引数なしのメソッドのresultSigがEmptyになる。Scala3の方の不具合?
         throw new NotImplementedError(s"Method without parameters not supported: $methodDeclaration")
      } else {
        Symbol.classSymbol(methodDeclaration.signature.resultSig).typeRef
      }
      MethodType(paramNames)(_ => paramTypeRepr, _ => resultTypeRepr)
    }
    def newMethodDeclarations(cls: Symbol) =
      notImplementedMethods.map { declaration =>
        val methodType = newMethodType(declaration)
        Symbol.newMethod(cls, declaration.name, methodType)
      }

    val parents = List(TypeTree.of[Object], TypeTree.of[T])
    val cls = Symbol.newClass(Symbol.spliceOwner, mockClassName, parents = parents.map(_.tpe), newMethodDeclarations, selfType = None)

    // TODO: メソッド以外のやつ(valで定義した変数等)もNotImplementedErrorで実装するようにする
    val newMethods = notImplementedMethods.map(_.name).map { methodName =>
      // TODO: 利便性をよくするためNotImplementedErrorで引数の値を表示するようにしたい
      DefDef(cls.declaredMethod(methodName).head, argss => Some('{ throw new NotImplementedError("Not stubed") }.asTerm))
    }
    val clsDef = ClassDef(cls, parents, body = newMethods)
    val newCls = Typed(Apply(Select(New(TypeIdent(cls)), cls.primaryConstructor), Nil), TypeTree.of[T])

    Block(List(clsDef), newCls).asExprOf[T]
  }
}
