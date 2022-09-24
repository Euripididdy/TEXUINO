package test.expandablelistdrawertest;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Stefan on 25/07/2017.
 */

public class Group
{

    public String string;
    public final List<String> children = new ArrayList<String>();

    public Group(String string)
    {
        this.string = string;
    }

}