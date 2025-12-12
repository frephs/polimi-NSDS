#include <mpi.h>
#include <stdio.h>
#include <stdlib.h>
#include <math.h>

// Group number:
// Group members:

const double L = 100.0;                 // Length of the 1d domain
const int n = 1000;                     // Total number of points
const int iterations_per_round = 1000;  // Number of iterations for each round of simulation
const double allowed_diff = 0.001;      // Stopping condition: maximum allowed difference between values

double initial_condition(double x, double L) {
    return fabs(x - L / 2);
}

int main(int argc, char** argv) {
    MPI_Init(&argc, &argv);

    int rank, size;
    MPI_Comm_rank(MPI_COMM_WORLD, &rank);
    MPI_Comm_size(MPI_COMM_WORLD, &size);

    // Variables declaration and initialization
    int local_n = n / size;
    double* points = malloc(sizeof(double) * local_n);

    // Set initial conditions
    for (int i = local_n * rank; i < local_n * (rank + 1); i++) {
        double x = L * i / (n - 1);
        points[i - local_n * rank] = initial_condition(x, L);
    }

    int round = 0;
    while (1) {
        // Perform one round of iterations
        round++;
        for (int t = 0; t < iterations_per_round; t++) {
            // Implement the code for each iteration

            // Exchange boundary data with neighboring processes
            double left_send = points[0];
            double right_send = points[local_n - 1];
            double left_recv, right_recv;

            MPI_Request requests[4];
            int req_count = 0;
            if (rank > 0) {
                MPI_Isend(&left_send, 1, MPI_DOUBLE, rank - 1, 0, MPI_COMM_WORLD, &requests[req_count++]);
                MPI_Irecv(&left_recv, 1, MPI_DOUBLE, rank - 1, 0, MPI_COMM_WORLD, &requests[req_count++]);
            }
            if (rank < size - 1) {
                MPI_Isend(&right_send, 1, MPI_DOUBLE, rank + 1, 0, MPI_COMM_WORLD, &requests[req_count++]);
                MPI_Irecv(&right_recv, 1, MPI_DOUBLE, rank + 1, 0, MPI_COMM_WORLD, &requests[req_count++]);
            }
            if (req_count > 0) {
                MPI_Waitall(req_count, requests, MPI_STATUSES_IGNORE);
            }

            for (int i = 0; i < local_n; i++) {
                double left = (i == 0) ? (rank == 0 ? points[i] : left_recv) : points[i - 1];
                double right = (i == local_n - 1) ? (rank == size - 1 ? points[i] : right_recv) : points[i + 1];

                points[i] = 
                    1.0 / ((i == 0 && rank == 0 || i == local_n - 1 && rank == size - 1) ? 2.0 : 3.0) * (((rank == 0 && i == 0) ? 0 : left) + points[i] + ((rank == size - 1 && i == local_n - 1) ? 0 : right));
            }
        }


        MPI_Barrier(MPI_COMM_WORLD);


        // Compute global minimum and maximum
        double local_min = points[0];
        double local_max = points[0];
        for (int i = 1; i < local_n; i++) {
            if (points[i] < local_min) local_min = points[i];
            if (points[i] > local_max) local_max = points[i];
        }
        
        double global_min, global_max;
        MPI_Allreduce(&local_min, &global_min, 1, MPI_DOUBLE, MPI_MIN, MPI_COMM_WORLD);
        MPI_Allreduce(&local_max, &global_max, 1, MPI_DOUBLE, MPI_MAX, MPI_COMM_WORLD);
        
        double max_diff = global_max - global_min;

        if (rank == 0) {
            printf("Round: %d\tMin: %.5f\tMax: %.5f\tDiff: %.5f\n", round, global_min, global_max, max_diff);
        }

        // TODO
        // Implement stopping conditions (break)
        if(global_max - global_min < allowed_diff) {
            if (rank == 0) {
                printf("Converged after %d rounds.\n", round);
            }
            break;
        }
    }

    // TODO 
    // Deallocation
    free(points);


    MPI_Finalize();
    return 0;
}
