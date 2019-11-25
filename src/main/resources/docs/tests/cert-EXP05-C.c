//#Patterns: cert-EXP05-C

void exp05()
{
    const int x = 42;

    int *p;
    //#Warn: cert-EXP05-C
    p = (int *)&x;
}
