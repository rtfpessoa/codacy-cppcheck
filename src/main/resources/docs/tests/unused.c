//#Patterns: unreadVariable,unusedFunction

int foo() {
   using namespace ::com::sun::star::i18n;
   bool b = false;
   int j = 0;
   for (int i = 0; i < 3; i++) {
          if (!b) {
             j = 3;
             b = true;
          }
   }
   return j;
}

struct ABC
{
    int a;
    int b;
    int c;
};

static struct ABC abc[] = { {1, 2, 3} };

//#Info: unusedFunction
void foo()
{
    //#Info: unreadVariable
    int a = abc[0].a;
    //#Info: unreadVariable
    int b = abc[0].b;
    //#Info: unreadVariable
    int c = abc[0].c;
}
