package com.github.sbaldin.tbot.data.enums

enum class BirdNameEnum(val id: Int, internal val title: String) {
    /**
     *  TODO replace with Eurasian three-toed woodpecker,
     *  We need to gather Dataset of https://en.wikipedia.org/wiki/Eurasian_three-toed_woodpecker
     *  But in first approximation it's ok https://en.wikipedia.org/wiki/American_three-toed_woodpecker
     */
    BIRD_0(id = 0, "1.American Three toed Woodpecker"),
    BIRD_1(id = 1, "10.Eurasian bullfinch"),
    BIRD_2(id = 2, "12.Pileated Woodpecker"),
    BIRD_3(id = 3, "13.Pine_Grosbeak"),
    BIRD_4(id = 4, "2.Bohemian Waxwing"),
    BIRD_5(id = 5, "3.Chestnut_sided_Warbler"),

    /**
     *  TODO replace with Eurasian Clark Nutcracker
     */
    BIRD_6(id = 6, "4.Clark Nutcracker"),
    BIRD_7(id = 7, "5.Common Cuckoo"),
    BIRD_8(id = 8, "7.Gadwall"),
    BIRD_9(id = 9, "9.common swift"),
    BIRD_10(id = 10, "AFRICAN CROWNED CRANE"),
    BIRD_11(id = 11, "ALBATROSS"),
    BIRD_12(id = 12, "AMERICAN PIPIT"),

    /**
     * American specific
     */
    BIRD_13(id = 13, "BALD EAGLE"),
    BIRD_14(id = 14, "BARN OWL"),
    BIRD_15(id = 15, "BLACK SWAN"),
    BIRD_16(id = 16, "BLACK-THROATED SPARROW"),

    /**
     * Find the russian endemic dataset, but now it's ok
     */
    BIRD_17(id = 17, "CALIFORNIA GULL"),
    BIRD_18(id = 18, "CHIPPING SPARROW"),
    BIRD_19(id = 19, "COMMON HOUSE MARTIN"),
    BIRD_20(id = 20, "CROW"),
    BIRD_21(id = 21, "CROWNED PIGEON"),
    BIRD_22(id = 22, "DOWNY WOODPECKER"),
    BIRD_23(id = 23, "EASTERN MEADOWLARK"),
    BIRD_24(id = 24, "EURASIAN MAGPIE"),
    BIRD_25(id = 25, "FLAMINGO"),

    /**
     * Probably should be replaced or removed(american specific)
     */
    BIRD_26(id = 26, "GILA WOODPECKER"),
    BIRD_27(id = 27, "GOLDEN EAGLE"),
    BIRD_28(id = 28, "GRAY PARTRIDGE"),
    BIRD_29(id = 29, "GUINEAFOWL"),
    BIRD_30(id = 30, "GYRFALCON"),
    BIRD_31(id = 31, "HARPY EAGLE"),
    BIRD_32(id = 32, "HOUSE SPARROW"),
    BIRD_33(id = 33, "JAVA SPARROW"),
    BIRD_34(id = 34, "JAVAN MAGPIE"),
    BIRD_35(id = 35, "LARK BUNTING"),
    BIRD_36(id = 36, "LONG-EARED OWL"),
    BIRD_37(id = 37, "MALLARD DUCK"),

    /**
     * Probably should be replaced or removed(american specific)
     */
    BIRD_38(id = 38, "MOURNING DOVE"),
    BIRD_39(id = 39, "MYNA"),
    BIRD_40(id = 40, "PARUS MAJOR"),
    BIRD_41(id = 41, "PELICAN"),
    BIRD_42(id = 42, "PEREGRINE FALCON"),

    /**
     * Probably should be replaced or removed(american specific)
     */
    BIRD_43(id = 43, "PHILIPPINE EAGLE"),

    /**
     * Probably should be replaced or removed(american specific)
     */
    BIRD_44(id = 44, "PURPLE MARTIN"),
    BIRD_45(id = 45, "RED HEADED WOODPECKER"),
    BIRD_46(id = 46, "RING-NECKED PHEASANT"),
    BIRD_47(id = 47, "ROCK DOVE"),
    BIRD_48(id = 48, "ROUGH LEG BUZZARD"),
    BIRD_49(id = 49, "SAMATRAN THRUSH"),
    BIRD_50(id = 50, "SAND MARTIN"),
    BIRD_51(id = 51, "SNOWY OWL"),
    BIRD_52(id = 52, "SRI LANKA BLUE MAGPIE"),
    BIRD_53(id = 53, "STEAMER DUCK"),
    BIRD_54(id = 54, "TAIWAN MAGPIE"),
    BIRD_55(id = 55, "TEAL DUCK"),
    BIRD_56(id = 56, "TRUMPTER SWAN"),
    BIRD_57(id = 57, "VARIED THRUSH"),
    BIRD_58(id = 58, "VULTURINE GUINEAFOWL");

    companion object {
        private val idMap: Map<Int, BirdNameEnum>

        init {
            val map = LinkedHashMap<Int, BirdNameEnum>(values().size)
            values().forEach { e ->
                if (map.put(e.id, e) != null) {
                    throw IllegalArgumentException("$e have duplicate ids: ${e.id}")
                }
            }
            idMap = map
        }

        fun fromId(id: Int): BirdNameEnum {
            return idMap[id] ?: throw IllegalArgumentException("BirdNameEnum does not have id $id")
        }
    }
}
