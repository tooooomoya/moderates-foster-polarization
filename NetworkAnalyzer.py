import networkx as nx
import pandas as pd
import numpy as np
import os
import powerlaw
import matplotlib.pyplot as plt
from collections import Counter

class NetworkMetricsAnalyzer:
    """
    指定したGEXFファイルを読み込み、ネットワークの主要な統計量を計算し、
    表示、CSV出力、および次数分布のプロットを行うヘルパークラス。
    """
    def __init__(self, gexf_path, output_csv=None, output_plot=None):
        self.gexf_path = gexf_path
        self.output_csv = output_csv
        self.output_plot = output_plot
        self.metrics = {}
        
        print(f"Loading GEXF file: {self.gexf_path} ...")
        self.G = nx.read_gexf(gexf_path)
        print("Load complete. Calculating metrics (this may take a while for large graphs)...\n")
        
        self._calculate_metrics()
        self.display_metrics()
        
        if self.output_csv:
            self.save_to_csv()
            
        if self.output_plot:
            self.plot_degree_distributions()

    def _fit_powerlaw(self, degrees):
        """0を除外した次数リストから冪乗指数(Gamma)を計算するヘルパー"""
        valid_degrees = [d for d in degrees if d > 0]
        if len(valid_degrees) > 0:
            try:
                fit = powerlaw.Fit(valid_degrees, discrete=True, verbose=False)
                return fit.power_law.alpha, valid_degrees
            except Exception as e:
                print(f"Power-law calculation failed: {e}")
                return np.nan, valid_degrees
        return np.nan, []

    def _calculate_metrics(self):
        G = self.G
        is_directed = G.is_directed()
        
        # 1. ノード数・エッジ数
        num_nodes = G.number_of_nodes()
        num_edges = G.number_of_edges()
        
        # 2. 平均次数・平均入次数
        degrees = [d for n, d in G.degree()]
        avg_degree = sum(degrees) / num_nodes if num_nodes > 0 else 0
        
        if is_directed:
            in_degrees = [d for n, d in G.in_degree()]
            out_degrees = [d for n, d in G.out_degree()]
            avg_in_degree = sum(in_degrees) / num_nodes if num_nodes > 0 else 0
        else:
            avg_in_degree = np.nan
            in_degrees = []
            out_degrees = []
            
        # 3. 平均クラスタ係数
        G_undirected = G.to_undirected() if is_directed else G
        avg_clustering = nx.average_clustering(G_undirected)
        
        # 4. 平均経路長・NW直径
        if num_nodes > 0:
            if is_directed:
                largest_cc = max(nx.weakly_connected_components(G), key=len)
            else:
                largest_cc = max(nx.connected_components(G), key=len)
            
            G_largest = G.subgraph(largest_cc).copy()
            G_largest_un = G_largest.to_undirected()
            
            try:
                avg_path_length = nx.average_shortest_path_length(G_largest_un)
                diameter = nx.diameter(G_largest_un)
            except nx.NetworkXError:
                avg_path_length = np.nan
                diameter = np.nan
        else:
            avg_path_length = np.nan
            diameter = np.nan

        # 5. モジュラリティ (Louvain法)
        try:
            communities = nx.community.louvain_communities(G_undirected)
            modularity = nx.community.modularity(G_undirected, communities)
        except Exception as e:
            print(f"Modularity calculation failed: {e}")
            modularity = np.nan

        # 6. 冪乗指数 (Gamma) の計算
        self.gamma_total, self.valid_degrees = self._fit_powerlaw(degrees)
        self.gamma_in, self.valid_in_degrees = self._fit_powerlaw(in_degrees) if is_directed else (np.nan, [])
        self.gamma_out, self.valid_out_degrees = self._fit_powerlaw(out_degrees) if is_directed else (np.nan, [])

        # 結果を辞書に格納
        self.metrics = {
            "File Name": os.path.basename(self.gexf_path),
            "Is Directed": is_directed,
            "Number of Nodes": num_nodes,
            "Number of Edges": num_edges,
            "Average Degree": avg_degree,
            "Average In-Degree": avg_in_degree,
            "Average Clustering Coefficient": avg_clustering,
            "Average Path Length (Largest CC)": avg_path_length,
            "Network Diameter (Largest CC)": diameter,
            "Modularity (Louvain)": modularity,
            "Power-law Gamma (Total)": self.gamma_total
        }
        
        if is_directed:
            self.metrics["Power-law Gamma (In)"] = self.gamma_in
            self.metrics["Power-law Gamma (Out)"] = self.gamma_out

    def _plot_scatter(self, ax, degree_list, title, gamma_val):
        """度数分布をカウント数で散布図としてプロットし、Gamma値を記載するヘルパー"""
        if not degree_list:
            ax.set_title(title)
            return

        # 確率ではなく純粋な出現回数(Count)を計算
        counts = Counter(degree_list)
        x = list(counts.keys())
        y = list(counts.values())

        # 散布図の描画 (両対数グラフ)
        ax.scatter(x, y, alpha=0.7, edgecolors='none', s=20)
        ax.set_xscale('log')
        ax.set_yscale('log')
        ax.set_xlabel('Degree')
        ax.set_ylabel('Count')
        ax.set_title(title)

        # Fit線の代わりにテキストでGamma値を記載
        if not np.isnan(gamma_val):
            text_str = f"$\\gamma$ = {gamma_val:.3f}"
            ax.text(0.95, 0.95, text_str, transform=ax.transAxes, fontsize=12,
                    verticalalignment='top', horizontalalignment='right',
                    bbox=dict(boxstyle='round', facecolor='white', alpha=0.8))

    def plot_degree_distributions(self):
        """次数分布の図を生成し保存する"""
        is_directed = self.G.is_directed()

        if is_directed:
            # 有向グラフ：3つの図を横並びに配置
            fig, axes = plt.subplots(1, 3, figsize=(15, 5))
            self._plot_scatter(axes[0], self.valid_in_degrees, "In-Degree Distribution", self.gamma_in)
            self._plot_scatter(axes[1], self.valid_out_degrees, "Out-Degree Distribution", self.gamma_out)
            self._plot_scatter(axes[2], self.valid_degrees, "Total Degree Distribution", self.gamma_total)
        else:
            # 無向グラフ：1つだけ配置
            fig, ax = plt.subplots(1, 1, figsize=(6, 5))
            self._plot_scatter(ax, self.valid_degrees, "Degree Distribution", self.gamma_total)

        plt.suptitle(f"Degree Distributions: {os.path.basename(self.gexf_path)}", fontsize=14)
        plt.tight_layout()
        
        plt.savefig(self.output_plot, dpi=300)
        plt.close()
        print(f"📈 Degree distribution plot saved to: {self.output_plot}")

    def display_metrics(self):
        print("="*50)
        print(f"Network Metrics: {self.metrics.get('File Name')}")
        print("="*50)
        for key, value in self.metrics.items():
            if isinstance(value, float):
                print(f"{key:<35}: {value:.4f}")
            else:
                print(f"{key:<35}: {value}")
        print("="*50)

    def save_to_csv(self):
        df = pd.DataFrame([self.metrics])
        file_exists = os.path.isfile(self.output_csv)
        df.to_csv(self.output_csv, mode='a', index=False, header=not file_exists, encoding='utf-8-sig')
        print(f"📊 Metrics successfully saved to: {self.output_csv}")

    def get_metrics_df(self):
        return pd.DataFrame([self.metrics])

# ==========================================
# 使い方 (実行例)
# ==========================================
if __name__ == "__main__":
    target_gexf = "./results/run_10_dir_-1.0/GEXF/lambda_0.0/step_5000.gexf" 
    
    output_csv_path = "results/nw_results/network_summary_stats.csv"
    output_plot_path = "results/nw_results/degree_distributions.png"  # <- 追加: プロットの保存先
    
    if os.path.exists(target_gexf):
        # output_plot を渡すと自動的に図の生成処理も走ります
        analyzer = NetworkMetricsAnalyzer(
            gexf_path=target_gexf, 
            output_csv=output_csv_path,
            output_plot=output_plot_path
        )
        
        df_stats = analyzer.get_metrics_df()
    else:
        print(f"Please place a '{target_gexf}' file in the current directory to test.")
        
    # 一括処理の例：
    # import glob
    # gexf_files = glob.glob("data/*.gexf")
    # for file in gexf_files:
    #     basename = os.path.splitext(os.path.basename(file))[0]
    #     NetworkMetricsAnalyzer(
    #         gexf_path=file, 
    #         output_csv="all_networks_stats.csv",
    #         output_plot=f"dist_{basename}.png"  # ファイルごとに画像名を変える
    #     )