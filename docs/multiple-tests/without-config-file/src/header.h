//#Patterns: unusedLabel
class alignas(alignof(void*)*4) Any {
    //#Info: unusedLabel
    public:
        void initialise(double _a, double _b);
        double add();
        double subtract();
    //#Info: unusedLabel
    private:
        double a;
        double b;
};
