package scalaparser

import acyclic.file
import language.implicitConversions
import syntax._
import fastparse._
/**
 * Parser for Scala syntax.
 */
object Scala extends Core with Types with Exprs with Xml{

  private implicit def wspStr(s: String) = R(WL ~ s)(Utils.literalize(s).toString)


  val TmplBody: R0 = {
    val Prelude = R( (Annot ~ OneNLMax).rep ~ (Mod ~! Pass).rep )
    val TmplStat = R( Import | Prelude ~ BlockDef | StatCtx.Expr )
    val SelfType = R( (`this` | Id | `_`) ~ (`:` ~ InfixType).? ~ `=>` )
    R( "{" ~! SelfType.? ~ Semis.? ~ TmplStat.rep(Semis) ~ `}` )
  }

  val ValVarDef = R( BindPattern.rep1(",") ~ (`:` ~! Type).? ~ (`=` ~! StatCtx.Expr).? )
  

  val FunDef = {
    val Body = R( `=` ~! `macro`.? ~ StatCtx.Expr | OneNLMax ~ "{" ~ Block ~ "}" )
    R( FunSig ~ (`:` ~! Type).? ~ Body.? )
  }

  val BlockDef: R0 = R( Dcl | TraitDef | ClsDef | ObjDef )

  val ClsDef = {
    val ClsAnnot = R( `@` ~ SimpleType ~ ArgList )
    val Prelude = R( NotNewline ~ ( ClsAnnot.rep1 ~ AccessMod.? | ClsAnnot.rep ~ AccessMod) )
    val ClsArgMod = R( (Mod.rep ~ (`val` | `var`)).? )
    val ClsArg = R( Annot.rep ~ ClsArgMod ~ Id ~ `:` ~ Type ~ (`=` ~ ExprCtx.Expr).? )

    val Implicit = R( OneNLMax ~ "(" ~ `implicit` ~ ClsArg.rep1(",") ~ ")" )
    val ClsArgs = R( OneNLMax ~ "(" ~ ClsArg.rep(",") ~ ")" )
    val AllArgs = R( ClsArgs.rep1 ~ Implicit.? | Implicit )
    R( `case`.? ~ `class` ~! Id ~ TypeArgList.? ~ Prelude.? ~ AllArgs.? ~ ClsTmplOpt )
  }
  val TraitDef = {
    val TraitTmplOpt = {
      val Constrs = R( (`with` ~ Constr).rep )
      val TraitParents = R( Constr ~ Constrs )
      val TraitTmpl = R( TmplBody ~ Constrs ~ TmplBody.?  | TraitParents ~ TmplBody.? | Pass )
      R( (`extends` | `<:`) ~ TraitTmpl | TmplBody | Pass )
    }
    R( `trait` ~! Id ~ TypeArgList.? ~ TraitTmplOpt )
  }

  val ObjDef: R0 = R( `case`.? ~ `object` ~! Id ~ ClsTmplOpt )
  val ClsTmplOpt: R0 = R( (`extends` | `<:`) ~ (ClsTmpl | TmplBody) | TmplBody.? )

  val Constr = R( AnnotType ~ (NotNewline ~ ParenArgList).rep )
  val ClsTmpl: R0 = {
    val ClsParents = R( Constr ~ (`with` ~ Constr).rep )
    val ClsBody = R( ClsParents ~ TmplBody.? )
    val EarlyDefBody = R( TmplBody ~ (`with` ~ ClsBody).? )
    R( EarlyDefBody | ClsBody )
  }

  val PkgObj = R( ObjDef )
  val PkgBlock = R( QualId ~! `{` ~ TopStatSeq.? ~ `}` )
  val TopStatSeq: R0 = {
    val Tmpl = R( (Annot ~ OneNLMax).rep ~ Mod.rep ~ (TraitDef | ClsDef | ObjDef) )
    val TopStat = R( `package` ~! (PkgBlock | PkgObj) | Import | Tmpl )
    R( TopStat.rep1(Semis) )
  }
  val TopPkgSeq = R( (`package` ~ QualId ~ !(WS ~ "{")).rep1(Semis) )
  val CompilationUnit: R0 = {
    val Body = R( TopPkgSeq ~ (Semis ~ TopStatSeq).? | TopStatSeq )
    R( Semis.? ~ Body.? ~ Semis.? ~ WL ~ Parser.End)
  }
}