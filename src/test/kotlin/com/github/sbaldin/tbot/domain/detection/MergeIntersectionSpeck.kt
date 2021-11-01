package com.github.sbaldin.tbot.domain.detection

import com.github.sbaldin.tbot.data.DetectedObjectModel
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import kotlin.test.assertEquals

class MergeIntersectionSpeck : Spek(
    {
        given("Merge Overlapping Object works Correctly") {
            on("Merge overlapping object called") {
                it("Empty List") {
                    assertEquals(emptyList(), emptyList<DetectedObjectModel>().mergeOverlapping())
                }
                it("Simple list with two overlapping rectangles") {
                    val merged = twoOverlappingRectangles().mergeOverlapping()
                    assertEquals(1, merged.size)
                    assertEquals(DetectedObjectModel(0, 0, 40, 40, 40, 40), merged.first())
                }
                it("Simple list of 3 rectangles") {
                    val notMerged = threeOverlappingRectanglesButAreaThresholdNotPassed().mergeOverlapping()
                    assertEquals(3, notMerged.size)
                    assertEquals(threeOverlappingRectanglesButAreaThresholdNotPassed().sortedBy { it.area }, notMerged.sortedBy { it.area })
                }

                it("list with two groups by 3 rectangles per group") {
                    val merged = twoGroupOfOverlappingRectangles().mergeOverlapping()
                    assertEquals(2, merged.size)
                    assertEquals(DetectedObjectModel(20, 20, 143, 61), merged.first())
                    assertEquals(DetectedObjectModel(235, 400, 285, 50), merged.last())
                }

                it("list with 3 groups, one single rect, 1 rectangle didnt pass are threshold and last two merged") {
                    val merged = threeGroupOfOverlappingRectangles().mergeOverlapping()
                    assertEquals(3, merged.size)
                    assertEquals(DetectedObjectModel(31, 8, 45, 17), merged.last())
                }
            }
        }
    },
)

private fun twoOverlappingRectangles() = listOf(
    DetectedObjectModel(
        5, 5, 40, 40,
    ),
    DetectedObjectModel(
        0, 0, 40, 40,
    ),
)

// no merge due to area threshold
private fun threeOverlappingRectanglesButAreaThresholdNotPassed() = listOf(
    DetectedObjectModel(
        12, 4, 24, 17,
    ),
    DetectedObjectModel(
        10, 10, 23, 16,
    ),
    DetectedObjectModel(
        8, 8, 14, 15,
    ),
)

// no merge due to area threshold
private fun twoGroupOfOverlappingRectangles() = listOf(
    DetectedObjectModel(
        20, 20, 140, 60,
    ),
    DetectedObjectModel(
        22, 24, 143, 61,
    ),
    DetectedObjectModel(
        25, 23, 134, 55,
    ),

    DetectedObjectModel(
        240, 400, 280, 40,
    ),
    DetectedObjectModel(
        235, 412, 277, 50,
    ),
    DetectedObjectModel(
        243, 406, 285, 43,
    ),
)

// should be 3 after merge
private fun threeGroupOfOverlappingRectangles() = listOf(
    DetectedObjectModel(
        10, 10, 12, 12,
    ),

    DetectedObjectModel(
        28, 6, 34, 14,
    ),
    DetectedObjectModel(
        31, 8, 43, 17,
    ),
    DetectedObjectModel(
        33, 9, 45, 17,
    ),
)

/*
private fun badCaseWithLast2Rect() =  listOf(
    DetectedObjectModel(topLeftX = 88, topLeftY = 390, bottomRightX = 251, bottomRightY = 604, width = 163, height = 214),
    DetectedObjectModel(topLeftX = 513, topLeftY = 342, bottomRightX = 952, bottomRightY = 643, width = 439, height = 301),
    DetectedObjectModel(topLeftX = 807, topLeftY = 293, bottomRightX = 1129, bottomRightY = 509, width = 322, height = 216),
    DetectedObjectModel(topLeftX = 784, topLeftY = 298, bottomRightX = 1164, bottomRightY = 542, width = 380, height = 244),
    DetectedObjectModel(topLeftX = 835, topLeftY = 301, bottomRightX = 1203, bottomRightY = 511, width = 368, height = 210),
    DetectedObjectModel(topLeftX = 804, topLeftY = 297, bottomRightX = 1246, bottomRightY = 529, width = 442, height = 232),
)*/
