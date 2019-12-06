package com.dfrobot.angelo.blunobasicdemo;

public class Vector3
{
    public double x, y, z;

    public Vector3()
    {

    }

    public Vector3(double x, double y, double z)
    {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public void add (double x, double y, double z)
    {
        this.x += x;
        this.y += y;
        this.z += z;
    }

    public void add (Vector3 v)
    {
        add(v.x, v.y, v.z);
    }

    public void sub (double x, double y, double z)
    {
        this.x -= x;
        this.y -= y;
        this.z -= z;
    }

    public void sub (Vector3 v)
    {
        sub(v.x, v.y, v.z);
    }

    public void mul (double x, double y, double z)
    {
        this.x *= x;
        this.y *= y;
        this.z *= z;
    }

    public void mul (Vector3 v)
    {
        mul(v.x, v.y, v.z);
    }

    public void div (double x, double y, double z)
    {
        this.x /= x;
        this.y /= y;
        this.z /= z;
    }

    public void div (Vector3 v)
    {
        div(v.x, v.y, v.z);
    }

    public void div(double n) {div(n,n,n);}

    public double len()
    {
        return Math.sqrt(x*x+y*y+z*z);
    }

    public static Vector3 div(Vector3 v, double n)
    {
        return new Vector3(v.x / n, v.y / n, v.z / n);
    }

    public String toString()
    {
        return "{" + x + ", " + y + ", " + z + "}";
    }
}
