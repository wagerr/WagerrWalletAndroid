package com.wagerrwallet.tools.util;

public class PositionPointer{
    private int pos = -1;

    public PositionPointer( int pos )
    {
        this.pos = pos;
    }

    public int getPos()     { return pos; }
    public void Up( int up ) { pos+=up; }
}
