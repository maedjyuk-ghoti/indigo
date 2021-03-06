package indigoextras.geometry

import indigo.shared.EqualTo._

import utest._
import indigo.shared.datatypes.Rectangle
import indigo.shared.datatypes.Point

object BoundingBoxTests extends TestSuite {

  val tests: Tests =
    Tests {
      "creating rectangles" - {

        "should be able to construct a bounding box from two vertices" - {
          val pt1 = Vertex(5, 6)
          val pt2 = Vertex(1, 3)

          val expected = BoundingBox(1, 3, 4, 3)

          BoundingBox.fromTwoVertices(pt1, pt2) === expected ==> true
        }

        "should be able to construct a bounding box from a cloud of vertices" - {
          //left 0, right 6, top 7, bottom 13
          val vertices: List[Vertex] =
            List(
              Vertex(4, 11),
              Vertex(6, 8),
              Vertex(2, 9),
              Vertex(1, 13),
              Vertex(3, 10),
              Vertex(0, 12),
              Vertex(5, 7)
            )

          val expected: BoundingBox =
            BoundingBox(0, 7, 6, 6)

          val actual: BoundingBox =
            BoundingBox.fromVertexCloud(vertices)

          actual === expected ==> true
        }

        "should be able to construct a bounding box from a rectangle" - {

          val rectangle = Rectangle(Point(10, 20), Point(30, 40))

          val actual = BoundingBox.fromRectangle(rectangle)

          val expected = BoundingBox(Vertex(10, 20), Vertex(30, 40))

          actual ==> expected
          actual.toRectangle ==> rectangle

        }

      }

      "Expand to include two bounding boxes" - {

        "should return the original bounding box when it already encompasses the second one" - {
          val a = BoundingBox(10, 20, 100, 200)
          val b = BoundingBox(20, 20, 50, 50)

          BoundingBox.expandToInclude(a, b) === a ==> true
        }

        "should expand to meet the bounds of both" - {
          val a = BoundingBox(10, 10, 20, 20)
          val b = BoundingBox(100, 100, 100, 100)

          BoundingBox.expandToInclude(a, b) === BoundingBox(10, 10, 190, 190) ==> true
        }

      }

      "intersecting vertices" - {

        "should be able to detect if the point is inside the BoundingBox" - {
          BoundingBox(0, 0, 10, 10).isVertexWithin(Vertex(5, 5)) ==> true
        }

        "should be able to detect that a point is outside the BoundingBox" - {
          BoundingBox(0, 0, 10, 10).isVertexWithin(Vertex(20, 5)) ==> false
        }

      }

      "Convert bounding box to line segments" - {
        val expected =
          List(
            LineSegment(Vertex(0, 0), Vertex(0, 3)),
            LineSegment(Vertex(0, 3), Vertex(3, 3)),
            LineSegment(Vertex(3, 3), Vertex(3, 0)),
            LineSegment(Vertex(3, 0), Vertex(0, 0))
          )

        val actual =
          BoundingBox(0d, 0d, 3d, 3d).toLineSegments

        actual ==> expected
      }

      "intersecting lines" - {

        "should find the intersection for the line passing through the bounding box" - {
          val actual =
            BoundingBox(1, 1, 3, 3)
              .lineIntersectsAt(
                LineSegment((1d, 0d), (4d, 4d))
              )
              .map { v =>
                // Round to 2 dp
                Vertex(Math.floor(v.x * 100) / 100, Math.floor(v.y * 100) / 100)
              }

          val expectd = Some(Vertex(1.74, 1))

          actual ==> expectd
        }

        "should not find intersection for a line outside the bounding box" - {
          BoundingBox(5, 5, 4, 4)
            .lineIntersectsAt(LineSegment((2d, 0d), (4d, 4d))) ==> None
        }

      }

      "encompasing rectangles" - {
        "should return true when A encompases B" - {
          val a = BoundingBox(10, 10, 110, 110)
          val b = BoundingBox(20, 20, 10, 10)

          BoundingBox.encompassing(a, b) ==> true
        }

        "should return false when A does not encompass B" - {
          val a = BoundingBox(20, 20, 10, 10)
          val b = BoundingBox(10, 10, 110, 110)

          BoundingBox.encompassing(a, b) ==> false
        }

        "should return false when A and B merely intersect" - {
          val a = BoundingBox(10, 10, 20, 200)
          val b = BoundingBox(15, 15, 100, 10)

          BoundingBox.encompassing(a, b) ==> false
        }
      }

      "overlapping bounding boxes" - {
        "should return true when A overlaps B" - {
          val a = BoundingBox(10, 10, 20, 20)
          val b = BoundingBox(15, 15, 100, 100)

          BoundingBox.overlapping(a, b) ==> true
        }

        "should return false when A and B do not overlap" - {
          val a = BoundingBox(10, 10, 20, 20)
          val b = BoundingBox(100, 100, 100, 100)

          BoundingBox.overlapping(a, b) ==> false
        }
      }

      "Expand" - {
        "should be able to expand in size by a given amount" - {
          val a = BoundingBox(10, 10, 20, 20)
          val b = BoundingBox(0, 10, 100, 5)

          BoundingBox.expand(a, 10) === BoundingBox(0, 0, 40, 40) ==> true
          BoundingBox.expand(b, 50) === BoundingBox(-50, -40, 200, 105) ==> true
        }
      }

    }
}
