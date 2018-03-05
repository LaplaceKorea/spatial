package spatial.lang.types

sealed trait BOOL[T] {
  val v: Boolean
  override def equals(x: Any): Boolean = x match {
    case that: BOOL[_] => this.v == that.v
    case _ => false
  }
  override def toString: String = if (v) "TRUE" else "FALSE"
}
sealed trait INT[T] {
  val v: Int
  override def equals(x: Any): Boolean = x match {
    case that: INT[_] => this.v == that.v
    case _ => false
  }
  override def toString = s"_$v"
}

trait TRUE extends BOOL[TRUE] { val v = true }
trait FALSE extends BOOL[FALSE] { val v = false }

trait _0 extends INT[ _0] { val v = 0 }
trait _1 extends INT[ _1] { val v = 1 }
trait _2 extends INT[ _2] { val v = 2 }
trait _3 extends INT[ _3] { val v = 3 }
trait _4 extends INT[ _4] { val v = 4 }
trait _5 extends INT[ _5] { val v = 5 }
trait _6 extends INT[ _6] { val v = 6 }
trait _7 extends INT[ _7] { val v = 7 }
trait _8 extends INT[ _8] { val v = 8 }
trait _9 extends INT[ _9] { val v = 9 }
trait _10 extends INT[_10] { val v = 10 }
trait _11 extends INT[_11] { val v = 11 }
trait _12 extends INT[_12] { val v = 12 }
trait _13 extends INT[_13] { val v = 13 }
trait _14 extends INT[_14] { val v = 14 }
trait _15 extends INT[_15] { val v = 15 }
trait _16 extends INT[_16] { val v = 16 }
trait _17 extends INT[_17] { val v = 17 }
trait _18 extends INT[_18] { val v = 18 }
trait _19 extends INT[_19] { val v = 19 }
trait _20 extends INT[_20] { val v = 20 }
trait _21 extends INT[_21] { val v = 21 }
trait _22 extends INT[_22] { val v = 22 }
trait _23 extends INT[_23] { val v = 23 }
trait _24 extends INT[_24] { val v = 24 }
trait _25 extends INT[_25] { val v = 25 }
trait _26 extends INT[_26] { val v = 26 }
trait _27 extends INT[_27] { val v = 27 }
trait _28 extends INT[_28] { val v = 28 }
trait _29 extends INT[_29] { val v = 29 }
trait _30 extends INT[_30] { val v = 30 }
trait _31 extends INT[_31] { val v = 31 }
trait _32 extends INT[_32] { val v = 32 }
trait _33 extends INT[_33] { val v = 33 }
trait _34 extends INT[_34] { val v = 34 }
trait _35 extends INT[_35] { val v = 35 }
trait _36 extends INT[_36] { val v = 36 }
trait _37 extends INT[_37] { val v = 37 }
trait _38 extends INT[_38] { val v = 38 }
trait _39 extends INT[_39] { val v = 39 }
trait _40 extends INT[_40] { val v = 40 }
trait _41 extends INT[_41] { val v = 41 }
trait _42 extends INT[_42] { val v = 42 }
trait _43 extends INT[_43] { val v = 43 }
trait _44 extends INT[_44] { val v = 44 }
trait _45 extends INT[_45] { val v = 45 }
trait _46 extends INT[_46] { val v = 46 }
trait _47 extends INT[_47] { val v = 47 }
trait _48 extends INT[_48] { val v = 48 }
trait _49 extends INT[_49] { val v = 49 }
trait _50 extends INT[_50] { val v = 50 }
trait _51 extends INT[_51] { val v = 51 }
trait _52 extends INT[_52] { val v = 52 }
trait _53 extends INT[_53] { val v = 53 }
trait _54 extends INT[_54] { val v = 54 }
trait _55 extends INT[_55] { val v = 55 }
trait _56 extends INT[_56] { val v = 56 }
trait _57 extends INT[_57] { val v = 57 }
trait _58 extends INT[_58] { val v = 58 }
trait _59 extends INT[_59] { val v = 59 }
trait _60 extends INT[_60] { val v = 60 }
trait _61 extends INT[_61] { val v = 61 }
trait _62 extends INT[_62] { val v = 62 }
trait _63 extends INT[_63] { val v = 63 }
trait _64 extends INT[_64] { val v = 64 }
trait _65 extends INT[_65] { val v = 65 }
trait _66 extends INT[_66] { val v = 66 }
trait _67 extends INT[_67] { val v = 67 }
trait _68 extends INT[_68] { val v = 68 }
trait _69 extends INT[_69] { val v = 69 }
trait _70 extends INT[_70] { val v = 70 }
trait _71 extends INT[_71] { val v = 71 }
trait _72 extends INT[_72] { val v = 72 }
trait _73 extends INT[_73] { val v = 73 }
trait _74 extends INT[_74] { val v = 74 }
trait _75 extends INT[_75] { val v = 75 }
trait _76 extends INT[_76] { val v = 76 }
trait _77 extends INT[_77] { val v = 77 }
trait _78 extends INT[_78] { val v = 78 }
trait _79 extends INT[_79] { val v = 79 }
trait _80 extends INT[_80] { val v = 80 }
trait _81 extends INT[_81] { val v = 81 }
trait _82 extends INT[_82] { val v = 82 }
trait _83 extends INT[_83] { val v = 83 }
trait _84 extends INT[_84] { val v = 84 }
trait _85 extends INT[_85] { val v = 85 }
trait _86 extends INT[_86] { val v = 86 }
trait _87 extends INT[_87] { val v = 87 }
trait _88 extends INT[_88] { val v = 88 }
trait _89 extends INT[_89] { val v = 89 }
trait _90 extends INT[_90] { val v = 90 }
trait _91 extends INT[_91] { val v = 91 }
trait _92 extends INT[_92] { val v = 92 }
trait _93 extends INT[_93] { val v = 93 }
trait _94 extends INT[_94] { val v = 94 }
trait _95 extends INT[_95] { val v = 95 }
trait _96 extends INT[_96] { val v = 96 }
trait _97 extends INT[_97] { val v = 97 }
trait _98 extends INT[_98] { val v = 98 }
trait _99 extends INT[_99] { val v = 99 }
trait _100 extends INT[_100] { val v = 100 }
trait _101 extends INT[_101] { val v = 101 }
trait _102 extends INT[_102] { val v = 102 }
trait _103 extends INT[_103] { val v = 103 }
trait _104 extends INT[_104] { val v = 104 }
trait _105 extends INT[_105] { val v = 105 }
trait _106 extends INT[_106] { val v = 106 }
trait _107 extends INT[_107] { val v = 107 }
trait _108 extends INT[_108] { val v = 108 }
trait _109 extends INT[_109] { val v = 109 }
trait _110 extends INT[_110] { val v = 110 }
trait _111 extends INT[_111] { val v = 111 }
trait _112 extends INT[_112] { val v = 112 }
trait _113 extends INT[_113] { val v = 113 }
trait _114 extends INT[_114] { val v = 114 }
trait _115 extends INT[_115] { val v = 115 }
trait _116 extends INT[_116] { val v = 116 }
trait _117 extends INT[_117] { val v = 117 }
trait _118 extends INT[_118] { val v = 118 }
trait _119 extends INT[_119] { val v = 119 }
trait _120 extends INT[_120] { val v = 120 }
trait _121 extends INT[_121] { val v = 121 }
trait _122 extends INT[_122] { val v = 122 }
trait _123 extends INT[_123] { val v = 123 }
trait _124 extends INT[_124] { val v = 124 }
trait _125 extends INT[_125] { val v = 125 }
trait _126 extends INT[_126] { val v = 126 }
trait _127 extends INT[_127] { val v = 127 }
trait _128 extends INT[_128] { val v = 128 }

/** Hack for working with customized bit widths, since Scala doesn't support integers as template parameters **/
object BOOL {
  def apply[T:BOOL]: BOOL[T] = implicitly[BOOL[T]]

  implicit lazy val BOOL_TRUE: BOOL[TRUE] = new TRUE {}
  implicit lazy val BOOL_FALSE: BOOL[FALSE] = new FALSE {}
}

object INT {
  def apply[T](implicit int: INT[T]): INT[T] = int
  def from[T](x: Int): INT[T] = new INT[T] { val v: Int = x }

  implicit lazy val INT0  : INT[_0  ] = new _0 {}
  implicit lazy val INT1  : INT[_1  ] = new _1 {}
  implicit lazy val INT2  : INT[_2  ] = new _2 {}
  implicit lazy val INT3  : INT[_3  ] = new _3 {}
  implicit lazy val INT4  : INT[_4  ] = new _4 {}
  implicit lazy val INT5  : INT[_5  ] = new _5 {}
  implicit lazy val INT6  : INT[_6  ] = new _6 {}
  implicit lazy val INT7  : INT[_7  ] = new _7 {}
  implicit lazy val INT8  : INT[_8  ] = new _8 {}
  implicit lazy val INT9  : INT[_9  ] = new _9 {}
  implicit lazy val INT10 : INT[_10 ] = new _10 {}
  implicit lazy val INT11 : INT[_11 ] = new _11 {}
  implicit lazy val INT12 : INT[_12 ] = new _12 {}
  implicit lazy val INT13 : INT[_13 ] = new _13 {}
  implicit lazy val INT14 : INT[_14 ] = new _14 {}
  implicit lazy val INT15 : INT[_15 ] = new _15 {}
  implicit lazy val INT16 : INT[_16 ] = new _16 {}
  implicit lazy val INT17 : INT[_17 ] = new _17 {}
  implicit lazy val INT18 : INT[_18 ] = new _18 {}
  implicit lazy val INT19 : INT[_19 ] = new _19 {}
  implicit lazy val INT20 : INT[_20 ] = new _20 {}
  implicit lazy val INT21 : INT[_21 ] = new _21 {}
  implicit lazy val INT22 : INT[_22 ] = new _22 {}
  implicit lazy val INT23 : INT[_23 ] = new _23 {}
  implicit lazy val INT24 : INT[_24 ] = new _24 {}
  implicit lazy val INT25 : INT[_25 ] = new _25 {}
  implicit lazy val INT26 : INT[_26 ] = new _26 {}
  implicit lazy val INT27 : INT[_27 ] = new _27 {}
  implicit lazy val INT28 : INT[_28 ] = new _28 {}
  implicit lazy val INT29 : INT[_29 ] = new _29 {}
  implicit lazy val INT30 : INT[_30 ] = new _30 {}
  implicit lazy val INT31 : INT[_31 ] = new _31 {}
  implicit lazy val INT32 : INT[_32 ] = new _32 {}
  implicit lazy val INT33 : INT[_33 ] = new _33 {}
  implicit lazy val INT34 : INT[_34 ] = new _34 {}
  implicit lazy val INT35 : INT[_35 ] = new _35 {}
  implicit lazy val INT36 : INT[_36 ] = new _36 {}
  implicit lazy val INT37 : INT[_37 ] = new _37 {}
  implicit lazy val INT38 : INT[_38 ] = new _38 {}
  implicit lazy val INT39 : INT[_39 ] = new _39 {}
  implicit lazy val INT40 : INT[_40 ] = new _40 {}
  implicit lazy val INT41 : INT[_41 ] = new _41 {}
  implicit lazy val INT42 : INT[_42 ] = new _42 {}
  implicit lazy val INT43 : INT[_43 ] = new _43 {}
  implicit lazy val INT44 : INT[_44 ] = new _44 {}
  implicit lazy val INT45 : INT[_45 ] = new _45 {}
  implicit lazy val INT46 : INT[_46 ] = new _46 {}
  implicit lazy val INT47 : INT[_47 ] = new _47 {}
  implicit lazy val INT48 : INT[_48 ] = new _48 {}
  implicit lazy val INT49 : INT[_49 ] = new _49 {}
  implicit lazy val INT50 : INT[_50 ] = new _50 {}
  implicit lazy val INT51 : INT[_51 ] = new _51 {}
  implicit lazy val INT52 : INT[_52 ] = new _52 {}
  implicit lazy val INT53 : INT[_53 ] = new _53 {}
  implicit lazy val INT54 : INT[_54 ] = new _54 {}
  implicit lazy val INT55 : INT[_55 ] = new _55 {}
  implicit lazy val INT56 : INT[_56 ] = new _56 {}
  implicit lazy val INT57 : INT[_57 ] = new _57 {}
  implicit lazy val INT58 : INT[_58 ] = new _58 {}
  implicit lazy val INT59 : INT[_59 ] = new _59 {}
  implicit lazy val INT60 : INT[_60 ] = new _60 {}
  implicit lazy val INT61 : INT[_61 ] = new _61 {}
  implicit lazy val INT62 : INT[_62 ] = new _62 {}
  implicit lazy val INT63 : INT[_63 ] = new _63 {}
  implicit lazy val INT64 : INT[_64 ] = new _64 {}
  implicit lazy val INT65 : INT[_65 ] = new _65 {}
  implicit lazy val INT66 : INT[_66 ] = new _66 {}
  implicit lazy val INT67 : INT[_67 ] = new _67 {}
  implicit lazy val INT68 : INT[_68 ] = new _68 {}
  implicit lazy val INT69 : INT[_69 ] = new _69 {}
  implicit lazy val INT70 : INT[_70 ] = new _70 {}
  implicit lazy val INT71 : INT[_71 ] = new _71 {}
  implicit lazy val INT72 : INT[_72 ] = new _72 {}
  implicit lazy val INT73 : INT[_73 ] = new _73 {}
  implicit lazy val INT74 : INT[_74 ] = new _74 {}
  implicit lazy val INT75 : INT[_75 ] = new _75 {}
  implicit lazy val INT76 : INT[_76 ] = new _76 {}
  implicit lazy val INT77 : INT[_77 ] = new _77 {}
  implicit lazy val INT78 : INT[_78 ] = new _78 {}
  implicit lazy val INT79 : INT[_79 ] = new _79 {}
  implicit lazy val INT80 : INT[_80 ] = new _80 {}
  implicit lazy val INT81 : INT[_81 ] = new _81 {}
  implicit lazy val INT82 : INT[_82 ] = new _82 {}
  implicit lazy val INT83 : INT[_83 ] = new _83 {}
  implicit lazy val INT84 : INT[_84 ] = new _84 {}
  implicit lazy val INT85 : INT[_85 ] = new _85 {}
  implicit lazy val INT86 : INT[_86 ] = new _86 {}
  implicit lazy val INT87 : INT[_87 ] = new _87 {}
  implicit lazy val INT88 : INT[_88 ] = new _88 {}
  implicit lazy val INT89 : INT[_89 ] = new _89 {}
  implicit lazy val INT90 : INT[_90 ] = new _90 {}
  implicit lazy val INT91 : INT[_91 ] = new _91 {}
  implicit lazy val INT92 : INT[_92 ] = new _92 {}
  implicit lazy val INT93 : INT[_93 ] = new _93 {}
  implicit lazy val INT94 : INT[_94 ] = new _94 {}
  implicit lazy val INT95 : INT[_95 ] = new _95 {}
  implicit lazy val INT96 : INT[_96 ] = new _96 {}
  implicit lazy val INT97 : INT[_97 ] = new _97 {}
  implicit lazy val INT98 : INT[_98 ] = new _98 {}
  implicit lazy val INT99 : INT[_99 ] = new _99 {}
  implicit lazy val INT100: INT[_100] = new _100 {}
  implicit lazy val INT101: INT[_101] = new _101 {}
  implicit lazy val INT102: INT[_102] = new _102 {}
  implicit lazy val INT103: INT[_103] = new _103 {}
  implicit lazy val INT104: INT[_104] = new _104 {}
  implicit lazy val INT105: INT[_105] = new _105 {}
  implicit lazy val INT106: INT[_106] = new _106 {}
  implicit lazy val INT107: INT[_107] = new _107 {}
  implicit lazy val INT108: INT[_108] = new _108 {}
  implicit lazy val INT109: INT[_109] = new _109 {}
  implicit lazy val INT110: INT[_110] = new _110 {}
  implicit lazy val INT111: INT[_111] = new _111 {}
  implicit lazy val INT112: INT[_112] = new _112 {}
  implicit lazy val INT113: INT[_113] = new _113 {}
  implicit lazy val INT114: INT[_114] = new _114 {}
  implicit lazy val INT115: INT[_115] = new _115 {}
  implicit lazy val INT116: INT[_116] = new _116 {}
  implicit lazy val INT117: INT[_117] = new _117 {}
  implicit lazy val INT118: INT[_118] = new _118 {}
  implicit lazy val INT119: INT[_119] = new _119 {}
  implicit lazy val INT120: INT[_120] = new _120 {}
  implicit lazy val INT121: INT[_121] = new _121 {}
  implicit lazy val INT122: INT[_122] = new _122 {}
  implicit lazy val INT123: INT[_123] = new _123 {}
  implicit lazy val INT124: INT[_124] = new _124 {}
  implicit lazy val INT125: INT[_125] = new _125 {}
  implicit lazy val INT126: INT[_126] = new _126 {}
  implicit lazy val INT127: INT[_127] = new _127 {}
  implicit lazy val INT128: INT[_128] = new _128 {}
}

trait CustomBitWidths {
  type INT[T] = spatial.lang.types.INT[T]
  lazy val INT = spatial.lang.types.INT
  type BOOL[T] = spatial.lang.types.BOOL[T]
  lazy val BOOL = spatial.lang.types.BOOL

  type TRUE = spatial.lang.types.TRUE
  type FALSE = spatial.lang.types.FALSE

  type _0 = spatial.lang.types._0
  type _1 = spatial.lang.types._1
  type _2 = spatial.lang.types._2
  type _3 = spatial.lang.types._3
  type _4 = spatial.lang.types._4
  type _5 = spatial.lang.types._5
  type _6 = spatial.lang.types._6
  type _7 = spatial.lang.types._7
  type _8 = spatial.lang.types._8
  type _9 = spatial.lang.types._9
  type _10 = spatial.lang.types._10
  type _11 = spatial.lang.types._11
  type _12 = spatial.lang.types._12
  type _13 = spatial.lang.types._13
  type _14 = spatial.lang.types._14
  type _15 = spatial.lang.types._15
  type _16 = spatial.lang.types._16
  type _17 = spatial.lang.types._17
  type _18 = spatial.lang.types._18
  type _19 = spatial.lang.types._19
  type _20 = spatial.lang.types._20
  type _21 = spatial.lang.types._21
  type _22 = spatial.lang.types._22
  type _23 = spatial.lang.types._23
  type _24 = spatial.lang.types._24
  type _25 = spatial.lang.types._25
  type _26 = spatial.lang.types._26
  type _27 = spatial.lang.types._27
  type _28 = spatial.lang.types._28
  type _29 = spatial.lang.types._29
  type _30 = spatial.lang.types._30
  type _31 = spatial.lang.types._31
  type _32 = spatial.lang.types._32
  type _33 = spatial.lang.types._33
  type _34 = spatial.lang.types._34
  type _35 = spatial.lang.types._35
  type _36 = spatial.lang.types._36
  type _37 = spatial.lang.types._37
  type _38 = spatial.lang.types._38
  type _39 = spatial.lang.types._39
  type _40 = spatial.lang.types._40
  type _41 = spatial.lang.types._41
  type _42 = spatial.lang.types._42
  type _43 = spatial.lang.types._43
  type _44 = spatial.lang.types._44
  type _45 = spatial.lang.types._45
  type _46 = spatial.lang.types._46
  type _47 = spatial.lang.types._47
  type _48 = spatial.lang.types._48
  type _49 = spatial.lang.types._49
  type _50 = spatial.lang.types._50
  type _51 = spatial.lang.types._51
  type _52 = spatial.lang.types._52
  type _53 = spatial.lang.types._53
  type _54 = spatial.lang.types._54
  type _55 = spatial.lang.types._55
  type _56 = spatial.lang.types._56
  type _57 = spatial.lang.types._57
  type _58 = spatial.lang.types._58
  type _59 = spatial.lang.types._59
  type _60 = spatial.lang.types._60
  type _61 = spatial.lang.types._61
  type _62 = spatial.lang.types._62
  type _63 = spatial.lang.types._63
  type _64 = spatial.lang.types._64
  type _65 = spatial.lang.types._65
  type _66 = spatial.lang.types._66
  type _67 = spatial.lang.types._67
  type _68 = spatial.lang.types._68
  type _69 = spatial.lang.types._69
  type _70 = spatial.lang.types._70
  type _71 = spatial.lang.types._71
  type _72 = spatial.lang.types._72
  type _73 = spatial.lang.types._73
  type _74 = spatial.lang.types._74
  type _75 = spatial.lang.types._75
  type _76 = spatial.lang.types._76
  type _77 = spatial.lang.types._77
  type _78 = spatial.lang.types._78
  type _79 = spatial.lang.types._79
  type _80 = spatial.lang.types._80
  type _81 = spatial.lang.types._81
  type _82 = spatial.lang.types._82
  type _83 = spatial.lang.types._83
  type _84 = spatial.lang.types._84
  type _85 = spatial.lang.types._85
  type _86 = spatial.lang.types._86
  type _87 = spatial.lang.types._87
  type _88 = spatial.lang.types._88
  type _89 = spatial.lang.types._89
  type _90 = spatial.lang.types._90
  type _91 = spatial.lang.types._91
  type _92 = spatial.lang.types._92
  type _93 = spatial.lang.types._93
  type _94 = spatial.lang.types._94
  type _95 = spatial.lang.types._95
  type _96 = spatial.lang.types._96
  type _97 = spatial.lang.types._97
  type _98 = spatial.lang.types._98
  type _99 = spatial.lang.types._99
  type _100 = spatial.lang.types._100
  type _101 = spatial.lang.types._101
  type _102 = spatial.lang.types._102
  type _103 = spatial.lang.types._103
  type _104 = spatial.lang.types._104
  type _105 = spatial.lang.types._105
  type _106 = spatial.lang.types._106
  type _107 = spatial.lang.types._107
  type _108 = spatial.lang.types._108
  type _109 = spatial.lang.types._109
  type _110 = spatial.lang.types._110
  type _111 = spatial.lang.types._111
  type _112 = spatial.lang.types._112
  type _113 = spatial.lang.types._113
  type _114 = spatial.lang.types._114
  type _115 = spatial.lang.types._115
  type _116 = spatial.lang.types._116
  type _117 = spatial.lang.types._117
  type _118 = spatial.lang.types._118
  type _119 = spatial.lang.types._119
  type _120 = spatial.lang.types._120
  type _121 = spatial.lang.types._121
  type _122 = spatial.lang.types._122
  type _123 = spatial.lang.types._123
  type _124 = spatial.lang.types._124
  type _125 = spatial.lang.types._125
  type _126 = spatial.lang.types._126
  type _127 = spatial.lang.types._127
  type _128 = spatial.lang.types._128
}