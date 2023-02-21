//#Patterns: unusedLabel
class alignas(alignof(void*)*4) Any {
    public:
        void initialise(double _a, double _b);
        double add();
        double subtract();
    private:
        double a;
        double b;
};
