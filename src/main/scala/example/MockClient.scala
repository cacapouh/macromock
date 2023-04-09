package example


import macromock.MockFactory
import annotation.experimental

case class Hoge(id: Int)

trait HogeRepository {
  def findX(id: Int): Hoge

  def defaultImpl(id: Int) = 1
}

@experimental
@main def test: Unit = {
  trait MockedHogeRepository extends HogeRepository {
    // ここでモックしたいメソッドをオーバーライドする
  }

  val mocked = MockFactory.mock[MockedHogeRepository] // ここで未実装のメソッドが全てNotImplementedErrorで実装される
  /**
   * ↓ MockFactory.mock[MockedHogeRepository]のマクロ展開結果
   *
   * {
   * class MockTargetImpl extends java.lang.Object with MockedHogeRepository {
   * def findX(id: scala.Int): macromock.Hoge = throw new scala.NotImplementedError("Not mocked")
   * }
   *
   * (new MockTargetImpl(): MockedHogeRepository)
   * }
   */

  println(mocked.defaultImpl(111)) // 1
  println(scala.util.Try(mocked.findX(1))) // Failure(scala.NotImplementedError: Not mocked)
}
