package example


import macromock.StubFactory
import annotation.experimental

case class Hoge(id: Int)

trait HogeRepository {
  def findX(id: Int): Hoge

  def defaultImpl(id: Int) = 1
}

@experimental
@main def test: Unit = {
  trait StubedHogeRepository extends HogeRepository {
    // ここでスタブしたいメソッドをオーバーライドする
  }

  val stubed = StubFactory.stub[StubedHogeRepository] // ここで未実装のメソッドが全てNotImplementedErrorで実装される
  /**
   * ↓ StubFactory.stub[StubedHogeRepository]のマクロ展開結果
   *
   * {
   * class StubTargetImpl extends java.lang.Object with StubedHogeRepository {
   * def findX(id: scala.Int): macromock.Hoge = throw new scala.NotImplementedError("Not stubed")
   * }
   *
   * (new StubTargetImpl(): StubedHogeRepository)
   * }
   */

  println(stubed.defaultImpl(111)) // 1
  println(scala.util.Try(stubed.findX(1))) // Failure(scala.NotImplementedError: Not stubed)
}
