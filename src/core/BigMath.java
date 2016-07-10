package core;

/*
 *  Copyright (C) 2011 geoForge Project
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */


import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * @author Amadeus Sowerby
 * amadeus.Sowerby@gmail.com
 *
 *
 */
public class BigMath
{
   
   public static BigDecimal ddecToDms(int deg, int min, double sec, int scale)
   {

      BigDecimal bdlDeg = new BigDecimal(deg);
      BigDecimal bdlMin = new BigDecimal(min);
      BigDecimal bdlSec = new BigDecimal(sec);
      return bdlDeg.add(bdlMin.divide(NMB_60, scale, ROUNDING_MODE)).add(bdlSec.divide(NMB_3600, scale, java.math.RoundingMode.FLOOR));
   }
   
   public static BigDecimal degToRad(BigDecimal degAngle, int scale)
   {

      return degAngle.divide(BigMath.NMB_180, scale, ROUNDING_MODE).multiply(BigMath.pi(scale));
   }

   public static BigDecimal _radToDeg_(BigDecimal degAngle, int scale)
   {

      return degAngle.divide(BigMath.pi(scale), scale, ROUNDING_MODE).multiply(BigMath.NMB_180);
   }
   
   public static BigDecimal tan(BigDecimal angle, int scale)
   {
      return sin(angle, scale).divide(cos(angle, scale), scale, ROUNDING_MODE);
   }

   public static BigDecimal sin(BigDecimal angle, int scale)
   {
      double dblAngle = Double.parseDouble(angle.toString());
      double zero = 0;
      double pi_2 = Double.parseDouble(BigMath.divide(NMB_PI, NMB_2).toString());
     
      angle.subtract(BigMath.divide(NMB_PI, NMB_2));
      if (  Math.abs(dblAngle - zero) < 0.0000000001D)
      {
         return NMB_0;
      }
      if ( Math.abs(dblAngle - pi_2) < 0.0000000001D)
      {
         return NMB_1;
      }
      BigDecimal r = NMB_0;
      BigDecimal r__1 = NMB_0;
      int i = 0;
      BigDecimal p = NMB__1;

      p = p.multiply(NMB__1);

      iterations = 0;
      boolean more = true;
      while (more)
      {
         r__1 = r;

         int i_x2_p1 = 2 * i + 1;
         r = r.add(
                 (p.multiply(angle.pow(i_x2_p1))).divide(fact(i_x2_p1), scale, java.math.RoundingMode.FLOOR));
         p = p.multiply(NMB__1);
         i++;

         if (++iterations >= maxIterations)
         {
            System.out.println("Iter : " + i);
            more = false;
         }
         else if (r__1.equals(r))
         {
            more = false;
         }

      }
      return r;
   }

   public static BigDecimal cos(BigDecimal angle, int scale)
   {
     
      angle.subtract(BigMath.divide(NMB_PI, NMB_2));
      if ( (BigMath.divide(NMB_PI, NMB_2)).compareTo(angle) == 0)
      {
         return NMB_0;
      }
      if ( (NMB_0).compareTo(angle) == 0)
      {
         return NMB_1;
      }
     
     
      BigDecimal r = NMB_0;
      BigDecimal r__1 = NMB_0;
      int i = 0;
      BigDecimal p = NMB__1;
      p = p.multiply(NMB__1);


      iterations = 0;
      boolean more = true;
      while (more)
      {
         r__1 = r;
         int i_x2 = 2 * i;
         r = r.add(
                 (p.multiply(angle.pow(i_x2))).divide(fact(i_x2), scale, ROUNDING_MODE));
         p = p.multiply(NMB__1);
         i++;

         if (++iterations >= maxIterations)
         {
            System.out.println("Iter : " + i);
            more = false;
         }
         else if (r__1.equals(r))
         {
            more = false;
         }

      }
      return r;
   }

   public static BigDecimal fact(int nmb)
   {
      if (nmb == 0)
      {
         return NMB_1;
      }

      int n = nmb;
      BigDecimal res = new BigDecimal(nmb);
      BigDecimal bdlN;
      while (n > 1)
      {
         n--;
         bdlN = new BigDecimal(n);
         res = res.multiply(bdlN);

      }
      return res;

   }

   private static BigDecimal getInitialApproximation(BigDecimal n)
   {
      BigInteger integerPart = n.toBigInteger();
      int length = integerPart.toString().length();
      if ((length % 2) == 0)
      {
         length--;
      }
      length /= 2;
      BigDecimal guess = NMB_1.movePointRight(length);
      return guess;
   }

   static public BigDecimal sqrt(BigDecimal n, int scale)
   {

      // Make sure n is a positive number

      if (n.compareTo(NMB_0) <= 0)
      {
         throw new IllegalArgumentException();
      }

      BigDecimal initialGuess = getInitialApproximation(n);
      BigDecimal lastGuess = NMB_0;
      BigDecimal guess = new BigDecimal(initialGuess.toString());

      // Iterate

      iterations = 0;
      boolean more = true;
      while (more)
      {
         lastGuess = guess;
         guess = n.divide(guess, scale, ROUNDING_MODE);
         guess = guess.add(lastGuess);
         guess = guess.divide(NMB_2, scale, ROUNDING_MODE);

         error = n.subtract(guess.multiply(guess));
         if (++iterations >= maxIterations)
         {
            System.out.println("Iter : " + iterations );
            more = false;
         }
         else if (lastGuess.equals(guess))
         {
           
            more = error.abs().compareTo(NMB_1) >= 0;
         }
      }
      return guess;

   }

   static public BigDecimal log(BigDecimal n, int scale)
   {
      BigMath.setScale(scale);
      BigDecimal x = n;
     
      BigDecimal r = NMB_0;
      r.setScale(scale);
      BigDecimal r__1 = NMB_0;
      int i = 0;
     
      // Iterate
      boolean more = true;
      while (more)
      {
         r__1 = r;
         int i_x2_p1 = 2 * i + 1;
         BigDecimal dblI_x2_p1 = new BigDecimal(i_x2_p1);
         //r.setScale(scale, java.math.RoundingMode.FLOOR);
       
         r = r.add(
                 BigMath.divide(
                     BigMath.NMB_2,
                     dblI_x2_p1)
                 .multiply(
                 (BigMath.divide(
                     x.subtract(BigMath.NMB_1),
                     x.add(BigMath.NMB_1))
                 ).pow(i_x2_p1)));
         
         r = BigMath.divide(r, BigMath.NMB_1);
         
         if (i++ >= maxIterations)
         {
            System.out.println("Iter : " + i );
            more = false;
         }
         else if (r__1.equals(r))
         {
            more = false;
         }
      }
      r.setScale(scale, ROUNDING_MODE);
      return r;
   }

   static public BigDecimal exp(BigDecimal n, int scale)
   {
      BigDecimal r = NMB_0;
      BigDecimal r__1 = NMB_0;
      int i = 0;
      // Iterate
      boolean more = true;
      while (more)
      {
         r__1 = r;
         r = r.add(BigMath.divide(n.pow(i), fact(i)));


         if (i++ >= maxIterations)
         {
            System.out.println("Iter : " + i);
            more = false;
         }
         else if (r__1.equals(r))
         {
            more = false;
         }
      }

      return r;
   }

   static public BigDecimal pow(BigDecimal a, BigDecimal b, int scale)
   {
      return exp(b.multiply(log(a, scale)), scale);
   }
   
   
   static public BigDecimal atan(BigDecimal n, int scale)
   {
      BigDecimal r = NMB_0;
      BigDecimal r__1 = NMB_0;
      int i = 0;
      BigDecimal p = NMB__1;
      p = p.multiply(NMB__1);
     
      // Iterate
      boolean more = true;
      while (more)
      {
         int i_x2_p1 = 2 * i + 1;
         BigDecimal bdlI_x2_p1 = new BigDecimal(i_x2_p1);
         r__1 = r;
         r = r.add(p.multiply(BigMath.divide(n.pow(i_x2_p1), bdlI_x2_p1)));
         p = p.multiply(NMB__1);

         if (i++ >= maxIterations)
         {
            System.out.println("Iter : " + i);
            more = false;
         }
         else if (r__1.equals(r))
         {
            more = false;
         }
      }
     
      return r;
   }
   
   public static BigDecimal pi(int scale)
   {
      BigDecimal pi = NMB_PI;
      pi.setScale(scale, BigDecimal.ROUND_HALF_UP);
      return pi;
   }

   public static BigDecimal divide(BigDecimal num, BigDecimal den)
   {
      return num.divide(den, SCALE, ROUNDING_MODE);
   }

   public static void setScale(int scale)
   {
      SCALE = scale;
   }

   public static void main(String[] args)
   {
      System.out.println(cos(NMB_2, 100));
      System.out.println(Math.cos(2));

      System.out.println(sin(NMB_2, 100));
      System.out.println(Math.sin(2));

      System.out.println(sin(new BigDecimal("0.5"), 100));
      System.out.println(Math.sin(0.5));

      System.out.println(tan(new BigDecimal("0.5"), 100));
      System.out.println(Math.tan(0.5));


     
      System.out.println(exp(new BigDecimal("4"), 100));
      System.out.println(Math.exp(4));
     
      System.out.println(log(new BigDecimal("2"), SCALE));
      System.out.println(Math.log(2));

      System.out.println(atan(new BigDecimal("0.5"), 100));
      System.out.println(Math.atan(0.5));

      //System.out.println(sinbis(0.8,1000));
   }
   private static BigDecimal error;
   private static int iterations;
   private static int maxIterations = 200;
   private static int SCALE = 100;
   public final static BigDecimal NMB__1 = new BigDecimal("-1");
   public final static BigDecimal NMB_0 = new BigDecimal("0");
   public final static BigDecimal NMB_1 = new BigDecimal("1");
   public final static BigDecimal NMB_2 = new BigDecimal("2");
   public final static BigDecimal NMB_3 = new BigDecimal("3");
   public final static BigDecimal NMB_4 = new BigDecimal("4");
   public final static BigDecimal NMB_60 = new BigDecimal("60");
   public final static BigDecimal NMB_90 = new BigDecimal("90");
   public final static BigDecimal NMB_180 = new BigDecimal("180");
   public final static BigDecimal NMB_3600 = new BigDecimal("3600");
   public final static BigDecimal NMB_10_14 = new BigDecimal("10000000000000");
   
   
   
   public final static BigDecimal NMB_PI = new BigDecimal("3.141592653589793238462643383279502884197169399375105820974944592307816406286208998628034825342117067982148086");
   private final static BigDecimal _NMB_PI_2400 = new BigDecimal("3.14159265358979323846264338327950288419716939937510582097494459230781640628620899862803482534211706798214808651328230664709384460955058223172535940812848111745028410270193852110555964462294895493038196442881097566593344612847564823378678316527120190914564856692346034861045432664821339360726024914127372458700660631558817488152092096282925409171536436789259036001133053054882046652138414695194151160943305727036575959195309218611738193261179310511854807446237996274956735188575272489122793818301194912983367336244065664308602139494639522473719070217986094370277053921717629317675238467481846766940513200056812714526356082778577134275778960917363717872146844090122495343014654958537105079227968925892354201995611212902196086403441815981362977477130996051870721134999999837297804995105973173281609631859502445945534690830264252230825334468503526193118817101000313783875288658753320838142061717766914730359825349042875546873115956286388235378759375195778185778053217122680661300192787661119590921642019893809525720106548586327886593615338182796823030195203530185296899577362259941389124972177528347913151557485724245415069595082953311686172785588907509838175463746493931925506040092770167113900984882401285836160356370766010471018194295559619894676783744944825537977472684710404753464620804668425906949129331367702898915210475216205696602405803815019351125338243003558764024749647326391419927260426992279678235478163600934172164121992458631503028618297455570674983850549458858692699569092721079750930295532116534498720275596023648066549911988183479775356636980742654252786255181841757467289097777279380008164706001614524919217321721477235014144197356854816136115735255213347574184946843852332390739414333454776241686251898356948556209921922218427255025425688767179049460165346680498862723279178608578438382796797668145410095388378636095068006422512520511739298489608412848862694560424196528502221066118630674427862203919494504712371378696095636437191728746776465757396241389086583264599581339047802759009946576407895126946839835259570982582262052248940772671947826848260147699090264013639443745530506820349625245174939965143142980919065925093722169646151570985838741059788595977297549893016175392846813826868386894277415599185592524595395943104997252468084598727364469584865383673622262609912460805124388439045124413654976278079771569143599770012961608944169486855584840635342207222582848864815845602850");

   private static int ROUNDING_MODE =  BigDecimal.ROUND_DOWN;
}

